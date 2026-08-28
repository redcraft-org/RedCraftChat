package org.redcraft.redcraftchat.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.github.retrooper.packetevents.protocol.dialog.input.SingleOptionInputControl;
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

    public static final ResourceLocation OPEN_MAIL = new ResourceLocation("redcraftchat", "mail_open");
    public static final ResourceLocation BACK_TO_INBOX = new ResourceLocation("redcraftchat", "mail_inbox");
    public static final ResourceLocation COMPOSE = new ResourceLocation("redcraftchat", "mail_compose");
    public static final ResourceLocation SEND = new ResourceLocation("redcraftchat", "mail_send");
    public static final ResourceLocation REPLY = new ResourceLocation("redcraftchat", "mail_reply");

    public static final String MAIL_ID_KEY = "mail";
    public static final String RECIPIENT_INPUT = "recipient";
    public static final String RECIPIENT_PICK = "pick";

    /**
     * The dropdown entry meaning "not one of these, read the typed field".
     * A real id rather than an empty string, because nothing promises the
     * client accepts an empty option id and a rejected dialog is a kick.
     */
    public static final String RECIPIENT_OTHER = "other";
    public static final String MESSAGE_INPUT = "message";

    private static final int BUTTON_WIDTH_PX = 200;
    private static final int BODY_WIDTH_PX = 320;
    private static final int INPUT_WIDTH_PX = 300;
    private static final int MESSAGE_MAX_LENGTH = 512;

    private MailDialog() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static boolean isSupported(Player player) {
        return NativeDialogSelector.isSupported(player);
    }

    /** The list: one button per mail, then a way to write one. */
    public static boolean showInbox(Player player) {
        if (!isSupported(player)) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            List<PlayerMail> mails = MailMessagesManager.getPlayerMail(player, false);

            List<ActionButton> buttons = new ArrayList<>();
            for (PlayerMail mail : mails) {
                String sender = MailMessagesManager.getMailSenderDisplayName(mail);
                String body = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);
                String label = (mail.readAt == null ? "● " : "  ") + sender + "  " + preview(body);

                NBTCompound additions = new NBTCompound();
                additions.setTag(MAIL_ID_KEY, new NBTString(mail.internalId));
                buttons.add(new ActionButton(
                        new CommonButtonData(Component.text(label), null, BUTTON_WIDTH_PX),
                        new DynamicCustomAction(OPEN_MAIL, additions)));
            }

            List<DialogBody> body = mails.isEmpty()
                    ? line(ui(preferences, UiStrings.MAIL_NO_MAILS))
                    : Collections.emptyList();

            ActionButton compose = new ActionButton(
                    new CommonButtonData(Component.text(ui(preferences, UiStrings.MAIL_SEND_TITLE)),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(COMPOSE, null));

            CommonDialogData common = new CommonDialogData(
                    Component.text(ui(preferences, UiStrings.MAIL_INBOX_HEADER)),
                    null, true, false, DialogAction.NONE, body, Collections.emptyList());

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, compose, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not build the mail inbox for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    /** One mail, whole. The client wraps it, so nothing is truncated. */
    public static boolean showMail(Player player, PlayerMail mail) {
        if (!isSupported(player) || mail == null) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            String sender = MailMessagesManager.getMailSenderDisplayName(mail);
            String body = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

            List<DialogBody> content = new ArrayList<>(line(body));
            if (mail.originalLanguage != null && !body.equals(mail.message)) {
                // The original used to live in a hover, which is exactly the
                // thing the reader of a translation cannot get at
                content.addAll(line("[" + mail.originalLanguage.toUpperCase() + "] " + mail.message));
            }

            NBTCompound additions = new NBTCompound();
            additions.setTag(MAIL_ID_KEY, new NBTString(mail.internalId));

            List<ActionButton> buttons = new ArrayList<>();
            buttons.add(new ActionButton(
                    new CommonButtonData(Component.text(ui(preferences, UiStrings.MAIL_CLICK_TO_REPLY)),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(REPLY, additions)));

            ActionButton back = new ActionButton(
                    new CommonButtonData(Component.text(ui(preferences, UiStrings.SELECTOR_BACK)),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(BACK_TO_INBOX, null));

            CommonDialogData common = new CommonDialogData(
                    Component.text(sender), null, true, false,
                    DialogAction.NONE, content, Collections.emptyList());

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, back, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not show a mail to {}: {}",
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
                List<SingleOptionInputControl.Entry> options = new ArrayList<>();
                options.add(new SingleOptionInputControl.Entry(RECIPIENT_OTHER,
                        Component.text(ui(preferences, UiStrings.MAIL_SOMEBODY_ELSE)), true));
                for (Player online : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
                    if (!online.getUniqueId().equals(player.getUniqueId())) {
                        options.add(new SingleOptionInputControl.Entry(online.getUsername(),
                                Component.text(online.getUsername()), false));
                    }
                }
                inputs.add(new Input(RECIPIENT_PICK, new SingleOptionInputControl(
                        INPUT_WIDTH_PX, options,
                        Component.text(ui(preferences, UiStrings.MAIL_RECIPIENT)), true)));
                inputs.add(new Input(RECIPIENT_INPUT, new TextInputControl(
                        INPUT_WIDTH_PX, Component.text(ui(preferences, UiStrings.MAIL_RECIPIENT)),
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
                    new CommonButtonData(Component.text(ui(preferences, UiStrings.SELECTOR_SUBMIT)),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(SEND, additions)));

            ActionButton back = new ActionButton(
                    new CommonButtonData(Component.text(ui(preferences, UiStrings.SELECTOR_BACK)),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(BACK_TO_INBOX, null));

            String title = presetRecipient == null
                    ? ui(preferences, UiStrings.MAIL_SEND_TITLE)
                    : ui(preferences, UiStrings.MAIL_CLICK_TO_REPLY) + "  " + presetRecipient;

            CommonDialogData common = new CommonDialogData(
                    Component.text(title), null, true, false,
                    DialogAction.NONE, Collections.emptyList(), inputs);

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, back, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not open the mail composer for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    private static List<DialogBody> line(String text) {
        return Collections.singletonList(new PlainMessageDialogBody(
                new PlainMessage(Component.text(text), BODY_WIDTH_PX)));
    }

    private static String preview(String message) {
        String flat = message == null ? "" : message.replace('\n', ' ');
        return flat.length() <= 28 ? flat : flat.substring(0, 28) + "...";
    }

    private static String ui(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }
}
