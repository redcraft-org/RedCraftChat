package org.redcraft.redcraftchat.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * The mailbox as a native dialog, the same surface the language selector uses.
 *
 * The in-world panel is gone from this path. Dialogs give the two things mail
 * actually needs and a display entity could not: text the client lays out
 * itself, so a body is readable at any length, and real input fields, so
 * writing one does not have to be handed off to chat.
 *
 * Three screens, all MULTI_ACTION so the buttons carry custom actions the
 * proxy reads back: the list, one mail, and the compose form.
 */
public final class MailDialog {

    public static final ResourceLocation MENU = new ResourceLocation("redcraftchat", "mail_menu");
    public static final ResourceLocation BACK_TO_INBOX = new ResourceLocation("redcraftchat", "mail_inbox");
    public static final ResourceLocation COMPOSE = new ResourceLocation("redcraftchat", "mail_compose");
    public static final ResourceLocation SEND = new ResourceLocation("redcraftchat", "mail_send");
    public static final ResourceLocation REPLY = new ResourceLocation("redcraftchat", "mail_reply");
    public static final ResourceLocation MARK_READ = new ResourceLocation("redcraftchat", "mail_mark_read");

    public static final String MAIL_ID_KEY = "mail";
    public static final String RECIPIENT_INPUT = "recipient";

    public static final String MESSAGE_INPUT = "message";

    private static final int BUTTON_WIDTH_PX = 200;
    private static final int BODY_WIDTH_PX = 320;
    private static final int INPUT_WIDTH_PX = 300;
    private static final int MESSAGE_MAX_LENGTH = 512;

    /** A dialog is not a scrollable inbox, so the list is capped and says so. */
    private static final int MAX_SHOWN = 6;


    private MailDialog() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static boolean isSupported(Player player) {
        return NativeDialogSelector.isSupported(player);
    }

    /**
     * The front door: what is waiting, and the two things you can do.
     *
     * /mail used to open the list directly, which meant the unread count was
     * only ever visible by reading the list itself, and writing a mail meant
     * going through the inbox first to reach the button.
     */
    public static boolean showMenu(Player player) {
        if (!isSupported(player)) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            List<PlayerMail> mails = MailMessagesManager.getPlayerMail(player, false);

            int unread = 0;
            for (PlayerMail mail : mails) {
                if (mail.readAt == null) {
                    unread++;
                }
            }

            // The count is the whole reason this screen exists, so it is said
            // in the button rather than buried in the body
            Component inboxLabel = Component.text()
                    .append(Component.text(ui(preferences, UiStrings.MAIL_OPEN_INBOX), NamedTextColor.WHITE))
                    .append(Component.text("  "))
                    .append(unread > 0
                            ? Component.text(unreadLabel(preferences, unread), NamedTextColor.GOLD)
                                    .decorate(TextDecoration.BOLD)
                            : Component.text(ui(preferences, UiStrings.MAIL_ALL_READ), NamedTextColor.DARK_GRAY))
                    .build();

            List<ActionButton> buttons = new ArrayList<>();
            buttons.add(new ActionButton(new CommonButtonData(inboxLabel, null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(BACK_TO_INBOX, null)));
            buttons.add(new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.MAIL_SEND_TITLE), NamedTextColor.WHITE),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(COMPOSE, null)));

            ActionButton close = new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.SELECTOR_CLOSE), NamedTextColor.GRAY),
                            null, BUTTON_WIDTH_PX),
                    null);

            CommonDialogData common = new CommonDialogData(
                    Component.text(ui(preferences, UiStrings.MAIL_TITLE), NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD),
                    null, true, false, DialogAction.CLOSE, Collections.emptyList(), Collections.emptyList());

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, close, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not open the mail menu for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * The list: the mails as readable text, with a reply button each.
     *
     * The messages used to be the button labels, which meant reading your
     * mail off the face of a button and having it cut short to fit. The body
     * is where text belongs; the buttons do one thing and say so.
     *
     * Body and buttons are separate sections, so the rows are numbered and
     * the buttons carry the same numbers. That is the only thing tying them
     * together when two mails come from the same person.
     */
    public static boolean showInbox(Player player) {
        if (!isSupported(player)) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            List<PlayerMail> mails = MailMessagesManager.getPlayerMail(player, false);

            List<DialogBody> body = new ArrayList<>();
            List<ActionButton> buttons = new ArrayList<>();

            if (mails.isEmpty()) {
                body.add(new PlainMessageDialogBody(new PlainMessage(
                        Component.text(ui(preferences, UiStrings.MAIL_NO_MAILS), NamedTextColor.GRAY),
                        BODY_WIDTH_PX)));
            }

            int shown = 0;
            boolean anyUnread = false;
            // One reply button per person, not per mail. Three mails from the
            // same player used to make three identical buttons, and the most
            // recent of them is the one worth replying to.
            Map<String, PlayerMail> senders = new LinkedHashMap<>();

            for (PlayerMail mail : mails) {
                if (shown >= MAX_SHOWN) {
                    break;
                }
                shown++;
                boolean unread = mail.readAt == null;
                anyUnread |= unread;

                String sender = MailMessagesManager.getMailSenderDisplayName(mail);
                senders.putIfAbsent(sender, mail);

                String text = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

                Component header = Component.text()
                        .append(Component.text(shown + ". ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(sender,
                                unread ? NamedTextColor.YELLOW : NamedTextColor.AQUA)
                                .decorate(TextDecoration.BOLD))
                        .append(Component.text(sentAt(mail), NamedTextColor.GRAY))
                        .append(unread
                                ? Component.text(" \u25cf", NamedTextColor.GOLD)
                                : Component.empty())
                        .build();

                body.add(new PlainMessageDialogBody(new PlainMessage(header, BODY_WIDTH_PX)));
                // The message reads the same whether or not it has been
                // opened before, so it is not dimmed once read. Unread is
                // said by the sender colour and the dot, not by the body.
                body.add(new PlainMessageDialogBody(new PlainMessage(
                        Component.text(text, NamedTextColor.WHITE), BODY_WIDTH_PX)));
            }

            if (mails.size() > shown) {
                // Saying so beats a list that silently stops
                body.add(new PlainMessageDialogBody(new PlainMessage(
                        Component.text("+" + (mails.size() - shown), NamedTextColor.DARK_GRAY)
                                .decorate(TextDecoration.ITALIC),
                        BODY_WIDTH_PX)));
            }

            for (Map.Entry<String, PlayerMail> entry : senders.entrySet()) {
                NBTCompound additions = new NBTCompound();
                additions.setTag(MAIL_ID_KEY, new NBTString(entry.getValue().internalId));
                buttons.add(new ActionButton(
                        new CommonButtonData(
                                Component.text()
                                        .append(Component.text(ui(preferences, UiStrings.MAIL_REPLY) + " ",
                                                NamedTextColor.WHITE))
                                        .append(Component.text(entry.getKey(), NamedTextColor.AQUA))
                                        .build(),
                                null, BUTTON_WIDTH_PX),
                        new DynamicCustomAction(REPLY, additions)));
            }

            buttons.add(new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.MAIL_SEND_TITLE), NamedTextColor.WHITE),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(COMPOSE, null)));

            if (anyUnread) {
                buttons.add(new ActionButton(
                        new CommonButtonData(
                                Component.text(ui(preferences, UiStrings.MAIL_MARK_AS_READ),
                                        NamedTextColor.GREEN),
                                null, BUTTON_WIDTH_PX),
                        new DynamicCustomAction(MARK_READ, null)));
            }

            // The exit slot is the way out of this screen, which is the menu
            // rather than the game. It used to hold the compose button, so
            // the mailbox had no way out at all and only Escape closed it.
            ActionButton close = new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.SELECTOR_BACK), NamedTextColor.GRAY),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(MENU, null));

            CommonDialogData common = new CommonDialogData(
                    Component.text(ui(preferences, UiStrings.MAIL_INBOX_HEADER), NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD),
                    null, true, false, DialogAction.CLOSE, body, Collections.emptyList());

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, close, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not build the mail inbox for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Writing one: who, then what, answered together.
     *
     * A dropdown of who is online plus a free field for anyone else, because
     * the proxy can only enumerate connected players and mail is deliverable
     * to people who are not. When replying the recipient is already known, so
     * the picker is dropped and only the message is asked for.
     */
    public static boolean showCompose(Player player, String presetRecipient) {
        if (!isSupported(player)) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            List<Input> inputs = new ArrayList<>();

            if (presetRecipient == null) {
                inputs.add(new Input(RECIPIENT_INPUT, new TextInputControl(
                        INPUT_WIDTH_PX,
                        Component.text(ui(preferences, UiStrings.MAIL_RECIPIENT_NAME)),
                        true, "", 32, null)));
            }

            inputs.add(new Input(MESSAGE_INPUT, new TextInputControl(
                    INPUT_WIDTH_PX, Component.text(ui(preferences, UiStrings.MAIL_MESSAGE)),
                    true, "", MESSAGE_MAX_LENGTH,
                    new TextInputControl.MultilineOptions(4, null))));

            NBTCompound additions = new NBTCompound();
            if (presetRecipient != null) {
                additions.setTag(RECIPIENT_INPUT, new NBTString(presetRecipient));
            }

            List<ActionButton> buttons = new ArrayList<>();
            buttons.add(new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.SELECTOR_SUBMIT), NamedTextColor.GREEN)
                                    .decorate(TextDecoration.BOLD),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(SEND, additions)));

            ActionButton back = new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.SELECTOR_BACK), NamedTextColor.GRAY),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(BACK_TO_INBOX, null));

            String title = presetRecipient == null
                    ? ui(preferences, UiStrings.MAIL_SEND_TITLE)
                    : ui(preferences, UiStrings.MAIL_REPLY) + "  " + presetRecipient;

            CommonDialogData common = new CommonDialogData(
                    Component.text(title, NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    null, true, false,
                    DialogAction.NONE, Collections.emptyList(), inputs);

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, back, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not open the mail composer for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * When it arrived, or nothing at all if the row has no date. A mail with
     * no timestamp is not worth an empty gap in the header.
     */
    private static String sentAt(PlayerMail mail) {
        String at = MailMessagesManager.formatSentAt(mail.sentAt);
        return at == null ? "" : "  " + at;
    }

    /**
     * The unread count, with the number spliced into the translated string
     * rather than concatenated, because where the number sits in the sentence
     * is not the same in every language.
     */
    private static String unreadLabel(PlayerPreferences preferences, int unread) {
        return ui(preferences, UiStrings.MAIL_UNREAD_COUNT).replace("%count%", String.valueOf(unread));
    }

    private static String ui(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }
}
