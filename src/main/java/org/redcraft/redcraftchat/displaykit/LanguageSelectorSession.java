package org.redcraft.redcraftchat.displaykit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import io.schemat.displaykit.action.ActionIcon;
import io.schemat.displaykit.action.ActionMenuSession;
import io.schemat.displaykit.action.ActionPage;
import io.schemat.displaykit.action.ActionSpec;
import io.schemat.displaykit.math.Vec3d;
import io.schemat.displaykit.surface.Surface;
import io.schemat.displaykit.surface.SurfaceAnchor;
import io.schemat.displaykit.surface.SurfaceFocus;
import io.schemat.displaykit.surface.SurfaceLifecyclePolicy;
import io.schemat.displaykit.surface.widget.ActionMenuView;
import io.schemat.displaykit.surface.widget.ActionMenuViewStyle;
import io.schemat.displaykit.velocity.VelocityDisplayKit;
import io.schemat.displaykit.velocity.player.VelocityPlayerRef;
import io.schemat.displaykit.velocity.surface.VelocitySurfacePresentation;
import kotlin.Unit;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * One player's live in-world language selector.
 *
 * Two invariants rule the shape of this class. Labels are resolved BEFORE the
 * UI tree is built, because label resolution goes through the blocking
 * translation cache and DisplayKit trees must be assembled from ready values.
 * And every DisplayKit callback arrives on the library's UI thread, so
 * anything that blocks (the preference mutations, label re-resolution) is
 * bounced onto the proxy scheduler first.
 */
public class LanguageSelectorSession {

    private static final double ANCHOR_DISTANCE_BLOCKS = 2.5;
    private static final int SURFACE_WIDTH_PX = 220;
    private static final int SURFACE_HEIGHT_PX = 250;
    private static final float SURFACE_WIDTH_BLOCKS = 2.2f;

    private final Player player;
    private final boolean firstJoin;
    private final Runnable onClosed;

    // Both are written on the scheduler thread that builds the session and
    // read on the UI thread that owns them, so neither may be stale there
    private volatile ActionMenuSession actions;
    private volatile VelocitySurfacePresentation presentation;
    // One latch for both close paths: the presentation closing itself and a
    // dismiss racing it must run the teardown exactly once between them
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public LanguageSelectorSession(Player player, PlayerPreferences preferences, boolean firstJoin,
            Runnable onClosed) {
        this.player = player;
        this.firstJoin = firstJoin;
        this.onClosed = onClosed;

        // Blocking: runs on the scheduler thread that created the session
        ActionPage page = buildPage(preferences);
        this.actions = new ActionMenuSession(page);
    }

    /** Builds the page with every label already resolved. */
    private ActionPage buildPage(PlayerPreferences preferences) {
        String title = PlayerPreferencesManager.localizeMessageForPlayer(preferences, UiStrings.SELECTOR_TITLE);
        String doneLabel = PlayerPreferencesManager.localizeMessageForPlayer(preferences, UiStrings.SELECTOR_DONE);

        Map<String, SupportedLocale> locales = new LinkedHashMap<>();
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            locales.put(locale.code, locale);
        }

        List<ActionSpec> specs = new ArrayList<>();
        for (SupportedLocale locale : locales.values()) {
            boolean isMain = locale.code.equalsIgnoreCase(preferences.mainLanguage);
            String endonym = LocaleManager.getEndonym(locale);

            specs.add(new ActionSpec(
                    "lang/" + locale.code,
                    endonym,
                    languageIcon(locale),
                    null,
                    true,
                    !isMain,
                    isMain,
                    false,
                    null,
                    context -> onLanguageClicked(locale.code)));
        }

        specs.add(new ActionSpec(
                "selector/done",
                doneLabel,
                new ActionIcon.Text("✔", ActionIcon.Default.INSTANCE),
                null,
                true,
                true,
                false,
                false,
                null,
                context -> onDoneClicked()));

        return new ActionPage("rcc:language-selector", specs, title, java.util.Collections.emptyList());
    }

    /**
     * The look vector with pitch removed, normalized. Falls back to due
     * south when the player is looking straight up or down and there is no
     * horizontal component to keep.
     */
    private static Vec3d flatten(Vec3d look) {
        double x = look.getX();
        double z = look.getZ();
        double length = Math.sqrt(x * x + z * z);
        if (length < 1e-4) {
            return new Vec3d(0.0, 0.0, 1.0);
        }
        return new Vec3d(x / length, 0.0, z / length);
    }

    private ActionIcon languageIcon(SupportedLocale locale) {
        String code = locale.code.split("-")[0].toUpperCase(java.util.Locale.ROOT);
        return new ActionIcon.Text(code, ActionIcon.Default.INSTANCE);
    }

    /** Presents the surface in front of the player. UI-thread hop inside. */
    public void present() {
        if (closed.get()) {
            // dismiss() beat us here: presenting now would spawn a surface
            // nothing tracks and nothing can close
            return;
        }
        VelocityDisplayKit displayKit = DisplayKitIntegration.get();
        VelocityPlayerRef owner = displayKit.playerRef(player);

        Vec3d eye = owner.eyePosition();
        Vec3d look = owner.lookDirection();
        if (eye.equals(Vec3d.Companion.getZERO())) {
            throw new IllegalStateException("No position known for the player yet");
        }

        // Flatten the look direction before anchoring. A player staring at
        // their feet when the prompt fires would otherwise get the panel
        // buried in the floor, where it is invisible, unclickable and
        // (PERSISTENT lifecycle) never times out, while still eating their
        // right-clicks through isTargetingInteractive.
        Vec3d facing = flatten(look);
        Vec3d center = eye.plus(facing.times(ANCHOR_DISTANCE_BLOCKS));

        Surface surface = new Surface(SURFACE_WIDTH_PX, SURFACE_HEIGHT_PX, Vec3d.Companion.getZERO(),
                SURFACE_WIDTH_BLOCKS, io.schemat.displaykit.render.Billboard.FIXED);

        UUID playerId = player.getUniqueId();
        ActionMenuView view = new ActionMenuView(
                "rcc-language-selector",
                actions,
                actionId -> SurfaceFocus.INSTANCE.state(playerId).getHoveredId() != null
                        && SurfaceFocus.INSTANCE.state(playerId).getHoveredId().equals(actionId),
                () -> Unit.INSTANCE,
                playerId,
                new ActionMenuViewStyle());

        surface.layout(root -> {
            root.addChild(view.getNode());
            return Unit.INSTANCE;
        });

        presentation = new VelocitySurfacePresentation(
                owner,
                surface,
                SurfaceAnchor.Companion.facing(center, facing),
                SurfaceLifecyclePolicy.PERSISTENT,
                null,
                "rcc-language-selector",
                () -> Unit.INSTANCE,
                reason -> {
                    markClosed();
                    return Unit.INSTANCE;
                });

        // Repaint whenever the action model changes; close when it closes
        // Fires synchronously on whichever thread mutated the session, which
        // is always the UI thread by construction now
        actions.subscribe(snapshot -> {
            if (snapshot.getClosed()) {
                closeQuietly();
            } else if (presentation != null && !closed.get()) {
                presentation.getSession().repaint();
            }
        }, false);

        presentation.present();

        // dismiss() can land between the check at the top of this method and
        // here. When it does it finds presentation still null, closes
        // nothing, and drops the session from the registry: the surface just
        // spawned would float there with nothing tracking it, eating the
        // player's right-clicks and never timing out on a PERSISTENT policy.
        if (closed.get()) {
            closeResources();
        }
    }

    /**
     * Runs a DisplayKit mutation on the library's UI owner thread.
     *
     * ActionMenuSession, the surface tree and the host are all confined to
     * that thread and hold no locks of their own; the 50 ms ticker is
     * already raycasting and repainting there. Anything touching them from a
     * scheduler or event thread races it.
     */
    private void onUiThread(Runnable task) {
        VelocityDisplayKit displayKit = DisplayKitIntegration.get();
        if (displayKit == null) {
            return;
        }
        // Wrapped here rather than at the call sites: a task handed to the
        // executor loses any exception into a Future nobody reads, so an
        // unwrapped failure would be invisible
        displayKit.getUiThread().dispatch(() -> {
            try {
                task.run();
            } catch (Exception | LinkageError e) {
                RedCraftChat.getInstance().getLogger().warn("Selector UI task failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        });
    }

    /** Language row clicked. Runs on the DisplayKit UI thread: hop off it. */
    private void onLanguageClicked(String localeCode) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                PlayerPreferencesManager.setMainPlayerLocale(preferences, localeCode);
                // The refresh arrives through onPreferencesUpdated
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Language selection failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    /** Done clicked: confirm the current language and dismiss. */
    private void onDoneClicked() {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                PlayerPreferencesManager.confirmLanguageSelection(preferences);
                BasicMessageFormatter.sendInternalMessage(player,
                        PlayerPreferencesManager.localizeMessageForPlayer(preferences, UiStrings.SELECTOR_CONFIRMED), NamedTextColor.GREEN);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Language confirm failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
            LanguageSelectorManager.dismiss(player.getUniqueId());
        }).schedule();
    }

    /**
     * Re-resolves labels in the player's current language and swaps the page
     * in place. Called from the preference-update hook; hops to a scheduler
     * thread for the blocking label resolution.
     */
    public void refresh() {
        if (closed.get()) {
            return;
        }
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                if (closed.get()) {
                    return;
                }
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                // Blocking label resolution happens here, on the scheduler
                ActionPage page = buildPage(preferences);
                // The swap itself, and the repaint its subscription triggers,
                // belong to the UI thread
                onUiThread(() -> {
                    if (!closed.get()) {
                        actions.replaceCurrent(page, true);
                    }
                });
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Selector refresh failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    private void markClosed() {
        if (closed.compareAndSet(false, true)) {
            onClosed.run();
        }
    }

    public void closeQuietly() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeResources();
        onClosed.run();
    }

    /**
     * Closes whatever has been built so far, on the UI thread that owns it.
     * Tolerates a presentation that does not exist yet, since a dismiss can
     * arrive from a disconnect or server-switch thread before present() has
     * built one.
     */
    private void closeResources() {
        onUiThread(() -> {
            try {
                if (presentation != null) {
                    presentation.close();
                }
                actions.close();
            } catch (Exception e) {
                // Closing twice from both sides of a race is expected here
                RedCraftChat.getInstance().getLogger().debug("Selector close raced: {}", e.getMessage());
            }
        });
    }

    public boolean isFirstJoin() {
        return firstJoin;
    }
}
