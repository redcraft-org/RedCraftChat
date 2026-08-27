package org.redcraft.redcraftchat.displaykit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private ActionMenuSession actions;
    private VelocitySurfacePresentation presentation;
    private volatile boolean closed = false;

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

    private ActionIcon languageIcon(SupportedLocale locale) {
        String code = locale.code.split("-")[0].toUpperCase(java.util.Locale.ROOT);
        return new ActionIcon.Text(code, ActionIcon.Default.INSTANCE);
    }

    /** Presents the surface in front of the player. UI-thread hop inside. */
    public void present() {
        VelocityDisplayKit displayKit = DisplayKitIntegration.get();
        VelocityPlayerRef owner = displayKit.playerRef(player);

        Vec3d eye = owner.eyePosition();
        Vec3d look = owner.lookDirection();
        if (eye.equals(Vec3d.Companion.getZERO())) {
            throw new IllegalStateException("No position known for the player yet");
        }
        Vec3d center = eye.plus(look.times(ANCHOR_DISTANCE_BLOCKS));

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
                SurfaceAnchor.Companion.facing(center, look),
                SurfaceLifecyclePolicy.PERSISTENT,
                null,
                "rcc-language-selector",
                () -> Unit.INSTANCE,
                reason -> {
                    markClosed();
                    return Unit.INSTANCE;
                });

        // Repaint whenever the action model changes; close when it closes
        actions.subscribe(snapshot -> {
            if (snapshot.getClosed()) {
                closeQuietly();
            } else if (presentation != null) {
                presentation.getSession().repaint();
            }
        }, false);

        presentation.present();
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
        if (closed) {
            return;
        }
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                if (closed) {
                    return;
                }
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                ActionPage page = buildPage(preferences);
                // The subscription repaints once the page is swapped
                actions.replaceCurrent(page, true);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Selector refresh failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    private void markClosed() {
        if (!closed) {
            closed = true;
            onClosed.run();
        }
    }

    public void closeQuietly() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (presentation != null) {
                presentation.close();
            }
            actions.close();
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().debug("Selector close raced: {}", e.getMessage());
        }
        onClosed.run();
    }

    public boolean isFirstJoin() {
        return firstJoin;
    }
}
