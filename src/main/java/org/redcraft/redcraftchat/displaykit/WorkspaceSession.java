package org.redcraft.redcraftchat.displaykit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.velocitypowered.api.proxy.Player;

import io.schemat.displaykit.action.ActionIcon;
import io.schemat.displaykit.action.ActionMenuSession;
import io.schemat.displaykit.action.ActionPage;
import io.schemat.displaykit.action.ActionSpec;
import io.schemat.displaykit.math.Vec3d;
import io.schemat.displaykit.render.BlockStateRef;
import io.schemat.displaykit.surface.BlockButton;
import io.schemat.displaykit.surface.Surface;
import io.schemat.displaykit.surface.SurfaceAnchor;
import io.schemat.displaykit.surface.SurfaceFocus;
import io.schemat.displaykit.surface.SurfaceLifecyclePolicy;
import io.schemat.displaykit.surface.widget.ActionMenuLabels;
import io.schemat.displaykit.surface.widget.ActionMenuView;
import io.schemat.displaykit.surface.widget.ActionMenuViewStyle;
import io.schemat.displaykit.surface.widget.SurfaceWindow;
import io.schemat.displaykit.surface.widget.TabbedPage;
import io.schemat.displaykit.surface.widget.TabbedView;
import io.schemat.displaykit.velocity.VelocityDisplayKit;
import io.schemat.displaykit.velocity.player.VelocityPlayerRef;
import io.schemat.displaykit.velocity.surface.VelocitySurfacePresentation;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.messaging.ChatPrompt;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.messaging.MailView;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import kotlin.Unit;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * The mail window: an inbox and a way to write one, as tabs.
 *
 * Languages are deliberately not here. They have their own selector, they are
 * set once and rarely touched, and putting them behind a mail window would
 * only make both harder to find.
 *
 * Writing needs two answers, and the two clients give them differently. The
 * recipient is a list of buttons either way. The message is a text box on
 * Bedrock, which has one, and a chat prompt on Java, which does not: a
 * display entity cannot be typed into.
 *
 * Threading follows the same rule as the language panel. DisplayKit callbacks
 * arrive on its UI owner thread and everything here blocks on preferences,
 * mail or translation, so work hops to the proxy scheduler and the tree
 * mutations hop back.
 */
public class WorkspaceSession {

    /** The tabs. Values are the switch keys, so they must stay distinct. */
    public enum Tab {
        INBOX,
        SEND
    }

    private static final double ANCHOR_DISTANCE_BLOCKS = 2.5;
    private static final float WINDOW_WIDTH_BLOCKS = 3.4f;
    private static final int WINDOW_MIN_WIDTH_PX = 420;
    private static final int WINDOW_MIN_HEIGHT_PX = 260;
    private static final int MENU_GAP_PX = 2;
    private static final int MENU_ROWS = 6;

    private final Player player;
    private final Runnable onClosed;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Tab selected;
    private volatile ActionMenuSession mailActions;
    private volatile ActionMenuSession sendActions;
    private volatile VelocitySurfacePresentation presentation;
    private volatile ActionMenuLabels navigationLabels = new ActionMenuLabels();

    public WorkspaceSession(Player player, PlayerPreferences preferences, Tab initialTab, Runnable onClosed) {
        this.player = player;
        this.onClosed = onClosed;
        this.selected = initialTab;

        // Blocking, and deliberately done here on the scheduler thread that
        // built the session rather than lazily while painting
        resolveChrome(preferences);
        this.mailActions = new ActionMenuSession(buildMailPage(preferences));
        this.sendActions = new ActionMenuSession(buildSendPage(preferences));
    }

    /** Mail rows, newest first, each opening its body in place. */
    private ActionPage buildMailPage(PlayerPreferences preferences) {
        List<ActionSpec> specs = new ArrayList<>();
        List<PlayerMail> mails;
        try {
            mails = MailMessagesManager.getPlayerMail(player, false);
        } catch (Exception e) {
            mails = new ArrayList<>();
        }

        int slot = 0;
        for (PlayerMail mail : mails) {
            slot++;
            String sender = MailMessagesManager.getMailSenderDisplayName(mail);
            String body = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);
            String label = sender + "  " + preview(body);
            boolean unread = mail.readAt == null;
            String internalId = mail.internalId;

            specs.add(new ActionSpec(
                    "mail/" + internalId,
                    label,
                    new ActionIcon.Text(unread ? "*" : " ", ActionIcon.Default.INSTANCE),
                    null,
                    true,
                    true,
                    unread,
                    false,
                    null,
                    context -> onMailClicked(internalId)));
        }

        if (specs.isEmpty()) {
            specs.add(new ActionSpec(
                    "mail/empty",
                    ui(preferences, UiStrings.MAIL_NO_MAILS),
                    ActionIcon.Default.INSTANCE,
                    null,
                    true,
                    false,
                    false,
                    false,
                    null,
                    null));
        }

        return new ActionPage("rcc:workspace/mail", specs,
                ui(preferences, UiStrings.MAIL_INBOX_HEADER), java.util.Collections.emptyList());
    }

    /**
     * Who to write to: one row per online player.
     *
     * Only online players, because that is the only list the proxy can
     * enumerate. Somebody offline is still reachable by typing their name
     * into /mail send, which the empty row says.
     */
    private ActionPage buildSendPage(PlayerPreferences preferences) {
        List<ActionSpec> specs = new ArrayList<>();
        for (Player online : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            String name = online.getUsername();
            specs.add(new ActionSpec(
                    "send/" + name,
                    name,
                    ActionIcon.Default.INSTANCE,
                    null,
                    true,
                    true,
                    false,
                    false,
                    null,
                    context -> onRecipientChosen(name)));
        }

        if (specs.isEmpty()) {
            specs.add(new ActionSpec("send/empty", "/mail send <player> <message>",
                    ActionIcon.Default.INSTANCE, null, true, false, false, false, null, null));
        }

        return new ActionPage("rcc:workspace/send", specs,
                ui(preferences, UiStrings.MAIL_SEND_TITLE), java.util.Collections.emptyList());
    }

    /** Opens the window in front of the player. UI-thread hop inside. */
    public void present() {
        if (closed.get()) {
            return;
        }
        VelocityDisplayKit displayKit = DisplayKitIntegration.get();
        VelocityPlayerRef owner = displayKit.playerRef(player);

        Vec3d eye = owner.eyePosition();
        Vec3d look = owner.lookDirection();
        if (eye.equals(Vec3d.Companion.getZERO())) {
            throw new IllegalStateException("No position known for the player yet");
        }
        Vec3d facing = flatten(look);
        Vec3d center = eye.plus(facing.times(ANCHOR_DISTANCE_BLOCKS));

        SurfaceWindow window = SurfaceWindow.Companion.vanilla(
                WINDOW_MIN_WIDTH_PX, WINDOW_MIN_HEIGHT_PX, new io.schemat.displaykit.surface.widget.SurfaceWindowStyle());

        Surface surface = new Surface(window.getSize().getW(), window.getSize().getH(),
                Vec3d.Companion.getZERO(), WINDOW_WIDTH_BLOCKS,
                io.schemat.displaykit.render.Billboard.FIXED);

        UUID playerId = player.getUniqueId();
        ActionMenuView mailView = menuView("rcc-workspace-mail", mailActions, playerId);
        ActionMenuView sendView = menuView("rcc-workspace-send", sendActions, playerId);

        List<TabbedPage<Tab>> pages = new ArrayList<>();
        pages.add(new TabbedPage<>(Tab.INBOX, tabLabel(UiStrings.MAIL_INBOX_HEADER), mailView.getNode()));
        pages.add(new TabbedPage<>(Tab.SEND, tabLabel(UiStrings.MAIL_SEND_TITLE), sendView.getNode()));

        TabbedView<Tab> tabs = new TabbedView<>(
                "rcc-workspace-tabs",
                pages,
                () -> selected,
                actionId -> isHovered(playerId, actionId),
                tab -> {
                    selected = tab;
                    return Unit.INSTANCE;
                },
                new io.schemat.displaykit.surface.widget.TabbedViewStyle());

        surface.layout(root -> {
            window.build(root, tabLabel(UiStrings.MAIL_INBOX_HEADER), () -> {
                closeQuietly();
                return Unit.INSTANCE;
            }, body -> {
                body.addChild(tabs.getNode());
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

        presentation = new VelocitySurfacePresentation(
                owner,
                surface,
                SurfaceAnchor.Companion.facing(center, facing),
                SurfaceLifecyclePolicy.PERSISTENT,
                null,
                "rcc-workspace",
                () -> Unit.INSTANCE,
                reason -> {
                    markClosed();
                    return Unit.INSTANCE;
                });

        repaintOnChange(mailActions);
        repaintOnChange(sendActions);

        presentation.present();

        if (closed.get()) {
            closeResources();
        }
    }

    private void repaintOnChange(ActionMenuSession session) {
        session.subscribe(snapshot -> {
            if (snapshot.getClosed()) {
                closeQuietly();
            } else if (presentation != null && !closed.get()) {
                presentation.getSession().repaint();
            }
        }, false);
    }

    private ActionMenuView menuView(String id, ActionMenuSession session, UUID playerId) {
        return new ActionMenuView(
                id,
                session,
                actionId -> isHovered(playerId, actionId),
                () -> Unit.INSTANCE,
                playerId,
                new ActionMenuViewStyle(
                        BlockButton.INSTANCE.widthFor(BlockButton.MIN_WIDTH),
                        MENU_ROWS,
                        () -> navigationLabels,
                        MENU_GAP_PX,
                        new BlockStateRef("minecraft:polished_blackstone"),
                        new BlockStateRef("minecraft:gilded_blackstone"),
                        new BlockStateRef("minecraft:gray_concrete"),
                        new BlockStateRef("minecraft:deepslate_tiles"),
                        0.0625f,
                        // The window's own title bar owns closing, so the menu
                        // does not need a second way out
                        false));
    }

    /** Opening a mail swaps the page for its body, with a way back. */
    private void onMailClicked(String internalId) {
        onScheduler(() -> {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            PlayerMail found = null;
            for (PlayerMail mail : MailMessagesManager.getPlayerMail(player, false)) {
                if (internalId.equals(mail.internalId)) {
                    found = mail;
                    break;
                }
            }
            if (found == null) {
                return;
            }
            if (found.readAt == null) {
                MailMessagesManager.markMailAsRead(found);
            }
            ActionPage detail = buildMailDetail(preferences, found);
            onUiThread(() -> {
                if (!closed.get()) {
                    mailActions.push(detail);
                }
            });
        });
    }

    /** The whole body, wrapped, with a reply hint and a way back to the list. */
    private ActionPage buildMailDetail(PlayerPreferences preferences, PlayerMail mail) {
        List<ActionSpec> specs = new ArrayList<>();
        String body = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

        int line = 0;
        for (String chunk : LanguageSelectorSession.wrap(body, 200, 6)) {
            specs.add(new ActionSpec("body/" + line++, chunk, ActionIcon.Default.INSTANCE,
                    null, true, false, false, false, null, null));
        }

        int slot = MailView.of(player.getUniqueId()).size();
        specs.add(new ActionSpec("mail/reply-hint",
                "/mail reply " + Math.max(1, slot) + " ...",
                ActionIcon.Default.INSTANCE, null, true, false, false, false, null, null));

        return new ActionPage("rcc:workspace/mail-detail", specs,
                MailMessagesManager.getMailSenderDisplayName(mail), java.util.Collections.emptyList());
    }

    /**
     * Recipient picked, so now the message.
     *
     * The panel cannot take typed input, so the question moves to chat and
     * the window closes: leaving it open over a player who is typing would
     * put a wall between them and what they are writing.
     */
    private void onRecipientChosen(String recipient) {
        onScheduler(() -> {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            closeQuietly();

            BasicMessageFormatter.sendInternalMessage(player,
                    ui(preferences, UiStrings.MAIL_TYPE_MESSAGE).replace("%player%", recipient),
                    NamedTextColor.AQUA);

            ChatPrompt.await(player, message -> sendMailTo(recipient, message));
        });
    }

    private void sendMailTo(String recipient, String message) {
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            PlayerPreferences target = PlayerPreferencesManager.getPlayerPreferences(recipient, true, false);
            if (target == null) {
                BasicMessageFormatter.sendInternalError(player, "That player was not found");
                return;
            }
            MailMessagesManager.sendMail(player, target.minecraftUuid, message);
            BasicMessageFormatter.sendInternalMessage(player,
                    ui(preferences, "Mail sent to ") + recipient, NamedTextColor.GREEN);
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not send mail for {}: {}",
                    player.getUsername(), e.getMessage());
        }
    }

    /** Rebuilds both tabs in the player's current language. */
    public void refresh() {
        if (closed.get()) {
            return;
        }
        onScheduler(() -> {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            resolveChrome(preferences);
            ActionPage mail = buildMailPage(preferences);
            ActionPage send = buildSendPage(preferences);
            onUiThread(() -> {
                if (closed.get()) {
                    return;
                }
                mailActions.popToRoot();
                mailActions.replaceCurrent(mail, true);
                sendActions.replaceCurrent(send, true);
            });
        });
    }

    private void resolveChrome(PlayerPreferences preferences) {
        navigationLabels = new ActionMenuLabels(
                ui(preferences, UiStrings.SELECTOR_BACK),
                ui(preferences, UiStrings.SELECTOR_CLOSE),
                ui(preferences, UiStrings.SELECTOR_PREVIOUS),
                ui(preferences, UiStrings.SELECTOR_NEXT));
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

    private void closeResources() {
        onUiThread(() -> {
            try {
                if (presentation != null) {
                    presentation.close();
                }
                mailActions.close();
                sendActions.close();
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().debug("Workspace close raced: {}", e.getMessage());
            }
        });
    }

    private boolean isHovered(UUID playerId, String actionId) {
        String hovered = SurfaceFocus.INSTANCE.state(playerId).getHoveredId();
        return hovered != null && hovered.equals(actionId);
    }

    private void onUiThread(Runnable task) {
        VelocityDisplayKit displayKit = DisplayKitIntegration.get();
        if (displayKit == null) {
            return;
        }
        displayKit.getUiThread().dispatch(() -> {
            try {
                task.run();
            } catch (Exception | LinkageError e) {
                RedCraftChat.getInstance().getLogger().warn("Workspace UI task failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        });
    }

    private interface Blocking {
        void run() throws Exception;
    }

    private void onScheduler(Blocking task) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Workspace action failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    private List<SupportedLocale> supportedLocales() {
        try {
            List<SupportedLocale> locales = LocaleManager.getSupportedLocales();
            return locales == null ? new ArrayList<>() : locales;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String tabLabel(String key) {
        try {
            return PlayerPreferencesManager.localizeUiForPlayer(
                    PlayerPreferencesManager.getPlayerPreferences(player), key);
        } catch (Exception e) {
            return key;
        }
    }

    private String ui(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }

    private static String preview(String message) {
        String flat = message == null ? "" : message.replace('\n', ' ');
        return flat.length() <= 22 ? flat : flat.substring(0, 22) + "...";
    }

    private static Vec3d flatten(Vec3d look) {
        double x = look.getX();
        double z = look.getZ();
        double length = Math.sqrt(x * x + z * z);
        if (length < 1e-4) {
            return new Vec3d(0.0, 0.0, 1.0);
        }
        return new Vec3d(x / length, 0.0, z / length);
    }
}
