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
import io.schemat.displaykit.render.DkColor;
import io.schemat.displaykit.render.TextMetrics;
import io.schemat.displaykit.surface.BlockButton;
import io.schemat.displaykit.surface.Surface;
import io.schemat.displaykit.surface.SurfaceAnchor;
import io.schemat.displaykit.surface.SurfaceFocus;
import io.schemat.displaykit.surface.SurfaceLifecyclePolicy;
import io.schemat.displaykit.surface.widget.ActionMenuView;
import io.schemat.displaykit.render.BlockStateRef;
import io.schemat.displaykit.surface.layout.CrossAxis;
import io.schemat.displaykit.surface.layout.FlexDirection;
import io.schemat.displaykit.surface.layout.FlexNode;
import io.schemat.displaykit.surface.layout.MainAxis;
import io.schemat.displaykit.surface.layout.PxSize;
import io.schemat.displaykit.surface.layout.WidgetNode;
import io.schemat.displaykit.surface.widget.ActionButtonStyle;
import io.schemat.displaykit.surface.widget.ActionButtonView;
import io.schemat.displaykit.surface.widget.ActionMenuLabels;
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
    private static final float SURFACE_WIDTH_BLOCKS = 2.2f;

    // An action slot the page does not fill still occupies its row and simply
    // paints nothing, which reads as a hole in the panel, so the menu holds
    // exactly one row per language and nothing else. The buttons that end a
    // step live below it, outside the list.
    private static final int MENU_ROW_HEIGHT_PX = 20;
    private static final int MENU_GAP_PX = 2;
    private static final int MENU_MAX_ROWS = 9;

    // Previous, Next and Back. They collapse to nothing when there is no page
    // to turn and the menu owns no Back, but each still contributes its gap.
    private static final int MENU_COLLAPSED_NAV_ROWS = 3;

    // Three lines of explanation above the menu. The panel's height is fixed
    // when it is built while the text is re-translated every time the player
    // switches language, so the budget has to cover the longest translation
    // rather than the English. German and Russian both run well past two
    // lines, and a truncated explanation is worse than a blank line.
    private static final int HELP_LINES = 3;
    private static final int HELP_PADDING_PX = 3;

    private static final String PAGE_PRIMARY = "rcc:language-selector/primary";
    private static final String PAGE_OTHERS = "rcc:language-selector/others";

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

    // Which step is showing. Tracked from the session's own snapshots rather
    // than set at each transition, so pressing Back keeps it honest and a
    // refresh rebuilds the step the player is actually looking at.
    private volatile String currentPageId = PAGE_PRIMARY;
    private final int menuRows;

    // Read while painting, so they are resolved on a scheduler thread and
    // published here rather than translated on the render path
    // The language this flow put in the understood list purely as a side
    // effect of it being picked as primary. Remembered so that changing the
    // primary takes it back out again, instead of leaving behind a language
    // the player only tried on the way to another one.
    private volatile String primaryAddedToUnderstood = null;

    private volatile List<String> helpLines = java.util.Collections.emptyList();
    private volatile ActionMenuLabels navigationLabels = new ActionMenuLabels();
    private volatile String stepButtonLabel = UiStrings.SELECTOR_NEXT;
    private volatile String closeButtonLabel = UiStrings.SELECTOR_CLOSE;
    private volatile boolean primaryChosen = false;

    public LanguageSelectorSession(Player player, PlayerPreferences preferences, boolean firstJoin,
            Runnable onClosed) {
        this.player = player;
        this.firstJoin = firstJoin;
        this.onClosed = onClosed;

        // Blocking: runs on the scheduler thread that created the session
        this.menuRows = menuRowsFor(LocaleManager.getSupportedLocales().size());
        resolveChrome(preferences, false);
        ActionPage page = buildPrimaryPage(preferences);
        this.actions = new ActionMenuSession(page);
    }

    /**
     * Rows the menu shows at once: every language plus the one button that
     * ends the step, clamped to what a player can comfortably read.
     */
    public static int menuRowsFor(int localeCount) {
        return Math.max(1, Math.min(localeCount, MENU_MAX_ROWS));
    }

    /**
     * Exact height of the whole panel: the help block, the menu, and the two
     * buttons under it, with every gap the column inserts.
     *
     * The collapsed navigation rows measure zero but still take a gap each,
     * which is why they appear here at all.
     */
    public static int surfaceHeightPx(int menuRows) {
        int menuChildren = 1 + menuRows + MENU_COLLAPSED_NAV_ROWS;
        int menuHeight = (1 + menuRows) * MENU_ROW_HEIGHT_PX + (menuChildren - 1) * MENU_GAP_PX;
        // help, menu, the step button, Close
        int columnGaps = 3 * MENU_GAP_PX;
        return helpHeightPx() + menuHeight + 2 * MENU_ROW_HEIGHT_PX + columnGaps;
    }

    /** Height of the explanation block above the menu. */
    public static int helpHeightPx() {
        return HELP_LINES * TextMetrics.FONT_LINE_HEIGHT_PX + HELP_PADDING_PX * 2;
    }

    /** The step the player is on, rebuilt with fresh labels. */
    private ActionPage buildCurrentPage(PlayerPreferences preferences) {
        return PAGE_OTHERS.equals(currentPageId) ? buildOthersPage(preferences) : buildPrimaryPage(preferences);
    }

    /**
     * Resolves the text the renderer paints itself, rather than the text the
     * action model carries. Blocking, so it belongs on a scheduler thread
     * alongside the page build.
     */
    private void resolveChrome(PlayerPreferences preferences, boolean others) {
        String help = PlayerPreferencesManager.localizeUiForPlayer(preferences,
                others ? UiStrings.SELECTOR_OTHERS_HELP : UiStrings.SELECTOR_PRIMARY_HELP);
        helpLines = wrap(help, SURFACE_WIDTH_PX - HELP_PADDING_PX * 2, HELP_LINES);
        navigationLabels = new ActionMenuLabels(
                PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_BACK),
                PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_CLOSE),
                PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_PREVIOUS),
                PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_NEXT));

        stepButtonLabel = PlayerPreferencesManager.localizeUiForPlayer(preferences,
                others ? UiStrings.SELECTOR_DONE : UiStrings.SELECTOR_NEXT);
        closeButtonLabel = others
                ? PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_BACK)
                : PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_CLOSE);
        primaryChosen = preferences.mainLanguage != null && !preferences.mainLanguage.isEmpty();
    }

    /**
     * Which language to drop from the understood list when the primary
     * changes: only ever the one this flow added itself, and never the
     * language being picked now.
     *
     * A language the player already understood before the selector opened is
     * left alone. They may simply be moving their primary and still read the
     * old one, and step two is right there to untick it if not.
     */
    public static String languageToDropOnPrimaryChange(String autoAdded, String newPrimary) {
        if (autoAdded == null || autoAdded.equalsIgnoreCase(newPrimary)) {
            return null;
        }
        return autoAdded;
    }

    /**
     * What to remember as self-added after picking a primary. Re-picking the
     * same one does not change how it got there.
     */
    public static String rememberAutoAdded(String autoAdded, String newPrimary, boolean alreadyUnderstood) {
        if (autoAdded != null && autoAdded.equalsIgnoreCase(newPrimary)) {
            return autoAdded;
        }
        return alreadyUnderstood ? null : newPrimary;
    }

    /**
     * Greedy word wrap against the real glyph widths, because a translation
     * is any length and the panel is a fixed 220px. The last line is
     * ellipsized rather than dropped, so an over-long translation degrades
     * instead of vanishing.
     */
    public static List<String> wrap(String text, int maxWidthPx, int maxLines) {
        List<String> all = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            // A single word wider than the panel still has to start a line;
            // ellipsize deals with it below
            if (line.length() == 0 || TextMetrics.INSTANCE.textWidthPx(candidate) <= maxWidthPx) {
                line.setLength(0);
                line.append(candidate);
            } else {
                all.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            all.add(line.toString());
        }

        if (all.size() <= maxLines) {
            // Every line fit as written, but a lone over-long word can still
            // overflow its own line
            List<String> exact = new ArrayList<>(all.size());
            for (String kept : all) {
                exact.add(TextMetrics.INSTANCE.ellipsize(kept, maxWidthPx));
            }
            return exact;
        }

        List<String> kept = new ArrayList<>(all.subList(0, maxLines));
        int last = maxLines - 1;
        // Words were dropped, so the last line has to show it. Appending the
        // first dropped word guarantees the line overflows, which is what
        // makes ellipsize add its marker rather than hand the text back
        // unchanged and hide the truncation.
        kept.set(last, TextMetrics.INSTANCE.ellipsize(
                kept.get(last) + " " + all.get(maxLines), maxWidthPx));
        return kept;
    }

    /**
     * The button that ends the step: Next on the first, Done on the second.
     *
     * Deliberately outside the menu. Inside it, it rendered as one more row
     * in the language list and read as a seventh language.
     */
    private ActionButtonView stepButton(UUID playerId) {
        return new ActionButtonView(
                "rcc-language-selector-step",
                () -> stepButtonLabel,
                () -> true,
                () -> primaryChosen,
                () -> false,
                new ActionButtonStyle(),
                // A different material from the language rows, so it reads as
                // the way forward rather than another thing to pick
                () -> new BlockStateRef("minecraft:gilded_blackstone"),
                actionId -> isHovered(playerId, actionId),
                () -> {
                    onStepButtonClicked();
                    return Unit.INSTANCE;
                });
    }

    /** Back on the second step, Close on the first. */
    private ActionButtonView closeButton(UUID playerId) {
        return new ActionButtonView(
                "rcc-language-selector-close",
                () -> closeButtonLabel,
                () -> true,
                () -> true,
                () -> false,
                new ActionButtonStyle(),
                () -> new BlockStateRef("minecraft:deepslate_tiles"),
                actionId -> isHovered(playerId, actionId),
                () -> {
                    onCloseClicked();
                    return Unit.INSTANCE;
                });
    }

    private boolean isHovered(UUID playerId, String actionId) {
        String hovered = SurfaceFocus.INSTANCE.state(playerId).getHoveredId();
        return hovered != null && hovered.equals(actionId);
    }

    /** The explanation block painted above the menu. */
    private WidgetNode helpNode() {
        WidgetNode node = new WidgetNode(
                "rcc-language-selector-help",
                new PxSize(SURFACE_WIDTH_PX, helpHeightPx()),
                (painter, rect) -> {
                    painter.fill(new DkColor(255, 32, 34, 40), rect);
                    List<String> lines = helpLines;
                    for (int i = 0; i < lines.size(); i++) {
                        painter.faceLabel(
                                lines.get(i),
                                rect.getX() + HELP_PADDING_PX,
                                TextMetrics.INSTANCE.rowAlignedY(
                                        rect.getY() + HELP_PADDING_PX + i * TextMetrics.FONT_LINE_HEIGHT_PX),
                                null,
                                0f);
                    }
                    return Unit.INSTANCE;
                });
        return node;
    }

    /**
     * Step one: pick the language the server speaks to you in. Exactly one
     * answer, so picking is what advances rather than a separate confirm.
     */
    private ActionPage buildPrimaryPage(PlayerPreferences preferences) {
        String title = PlayerPreferencesManager.localizeUiForPlayer(preferences,
                UiStrings.SELECTOR_PRIMARY_TITLE);

        List<ActionSpec> specs = new ArrayList<>();
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            boolean isMain = locale.code.equalsIgnoreCase(preferences.mainLanguage);
            specs.add(new ActionSpec(
                    "primary/" + locale.code,
                    LocaleManager.getEndonym(locale),
                    languageIcon(locale),
                    null,
                    true,
                    true,
                    isMain,
                    false,
                    null,
                    context -> onPrimaryClicked(locale.code)));
        }

        return new ActionPage(PAGE_PRIMARY, specs, title, java.util.Collections.emptyList());
    }

    /**
     * Step two: which of the rest they read without help. The main language is
     * left out on purpose, both because understanding it is implied and
     * because togglePlayerLocale refuses to remove it.
     */
    private ActionPage buildOthersPage(PlayerPreferences preferences) {
        String title = PlayerPreferencesManager.localizeUiForPlayer(preferences,
                UiStrings.SELECTOR_OTHERS_TITLE);
        List<ActionSpec> specs = new ArrayList<>();
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            boolean isMain = locale.code.equalsIgnoreCase(preferences.mainLanguage);
            boolean understood = isMain
                    || (preferences.languages != null && preferences.languages.contains(locale.code));
            // The main language stays listed, ticked and not clickable:
            // understanding it is implied, togglePlayerLocale refuses to
            // remove it, and leaving it in keeps both steps the same height
            specs.add(new ActionSpec(
                    "other/" + locale.code,
                    LocaleManager.getEndonym(locale),
                    languageIcon(locale),
                    null,
                    true,
                    !isMain,
                    understood,
                    false,
                    null,
                    context -> onOtherToggled(locale.code)));
        }

        return new ActionPage(PAGE_OTHERS, specs, title, java.util.Collections.emptyList());
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

        Surface surface = new Surface(SURFACE_WIDTH_PX, surfaceHeightPx(menuRows), Vec3d.Companion.getZERO(),
                SURFACE_WIDTH_BLOCKS, io.schemat.displaykit.render.Billboard.FIXED);

        UUID playerId = player.getUniqueId();
        ActionMenuView view = new ActionMenuView(
                "rcc-language-selector",
                actions,
                actionId -> SurfaceFocus.INSTANCE.state(playerId).getHoveredId() != null
                        && SurfaceFocus.INSTANCE.state(playerId).getHoveredId().equals(actionId),
                () -> Unit.INSTANCE,
                playerId,
                new ActionMenuViewStyle(
                        BlockButton.INSTANCE.widthFor(BlockButton.MIN_WIDTH),
                        menuRows,
                        // Read per repaint, so switching language on step one
                        // retranslates the buttons without rebuilding
                        () -> navigationLabels,
                        MENU_GAP_PX,
                        new BlockStateRef("minecraft:polished_blackstone"),
                        new BlockStateRef("minecraft:gilded_blackstone"),
                        new BlockStateRef("minecraft:gray_concrete"),
                        new BlockStateRef("minecraft:deepslate_tiles"),
                        0.0625f,
                        // The menu owns the language list and nothing else:
                        // its Back would sit below the step button
                        false));

        surface.layout(root -> {
            // The root is a BoxNode, which places every child at the same
            // origin, so two children painted straight onto it land on top of
            // each other. A column is what sequences them.
            FlexNode column = new FlexNode(
                    "rcc-language-selector-column",
                    FlexDirection.COLUMN,
                    MainAxis.START,
                    CrossAxis.START,
                    MENU_GAP_PX);
            column.addChild(helpNode());
            column.addChild(view.getNode());
            column.addChild(stepButton(playerId).getNode());
            column.addChild(closeButton(playerId).getNode());
            root.addChild(column);
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
            // Includes the pops the Back button drives, which nothing else
            // would tell this session about
            currentPageId = snapshot.getCurrentPage().getId();
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

    /**
     * Step one, a language picked. Sets it and stops there: the panel
     * repaints in that language so the choice is visible, and Next is what
     * moves on.
     *
     * Runs on the DisplayKit UI thread, so the blocking work hops to a
     * scheduler thread.
     */
    private void onPrimaryClicked(String localeCode) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

                boolean alreadyUnderstood = preferences.languages != null
                        && preferences.languages.contains(localeCode);
                String drop = languageToDropOnPrimaryChange(primaryAddedToUnderstood, localeCode);
                if (drop != null && preferences.languages != null) {
                    // In memory only: setMainPlayerLocale persists right
                    // after, so the swap costs one write rather than two
                    preferences.languages.remove(drop);
                }

                PlayerPreferencesManager.setMainPlayerLocale(preferences, localeCode);
                primaryAddedToUnderstood =
                        rememberAutoAdded(primaryAddedToUnderstood, localeCode, alreadyUnderstood);
                // onPreferencesUpdated -> refresh() repaints this step, now
                // in the language just chosen
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Language selection failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    /** The step button: Next on the first step, Done on the second. */
    private void onStepButtonClicked() {
        if (PAGE_OTHERS.equals(currentPageId)) {
            onDoneClicked();
        } else {
            onNextClicked();
        }
    }

    /** Close on the first step, Back on the second. */
    private void onCloseClicked() {
        if (PAGE_OTHERS.equals(currentPageId)) {
            onUiThread(() -> {
                if (!closed.get()) {
                    actions.pop();
                }
            });
            return;
        }
        LanguageSelectorManager.dismiss(player.getUniqueId());
    }

    /** Next on step one: build the second question and push it. */
    private void onNextClicked() {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                resolveChrome(preferences, true);
                ActionPage others = buildOthersPage(preferences);

                onUiThread(() -> {
                    if (closed.get()) {
                        return;
                    }
                    // push rather than replace, so Back returns to the
                    // primary choice instead of nowhere
                    if (PAGE_PRIMARY.equals(currentPageId)) {
                        actions.push(others);
                    } else {
                        actions.replaceCurrent(others, false);
                    }
                });
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Selector step failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    /** Step two: a language they do or do not read. The refresh hook repaints. */
    private void onOtherToggled(String localeCode) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                PlayerPreferencesManager.togglePlayerLocale(preferences, localeCode);
                // onPreferencesUpdated -> refresh() rebuilds this step with
                // the tick in its new state
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Language toggle failed for {}: {}",
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
                        PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SELECTOR_CONFIRMED), NamedTextColor.GREEN);
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
                resolveChrome(preferences, PAGE_OTHERS.equals(currentPageId));
                ActionPage page = buildCurrentPage(preferences);
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
