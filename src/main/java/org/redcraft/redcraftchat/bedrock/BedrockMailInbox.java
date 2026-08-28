package org.redcraft.redcraftchat.bedrock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.minecraft.BedrockPlayers;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * The inbox as native Bedrock forms.
 *
 * The chat inbox is unusable on Bedrock twice over: its paging, mark-as-read
 * and reply are clicks, and the body itself is truncated to a preview with
 * the rest in a hover. A Bedrock player could see that mail existed and read
 * neither it nor act on it.
 *
 * Here the list is a form, opening a mail shows the whole body, and replying
 * is a text box. Nothing is truncated and nothing needs a mouse.
 */
public final class BedrockMailInbox {

    private BedrockMailInbox() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static boolean isSupported(Player player) {
        return player != null && BedrockPlayers.isBedrock(player) && BedrockForms.isAvailable();
    }

    /** The list. Unread first, since that is what the player came for. */
    public static boolean showInbox(Player player, boolean unreadOnly) {
        if (!isSupported(player)) {
            return false;
        }

        PlayerPreferences preferences;
        List<PlayerMail> mails;
        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            mails = MailMessagesManager.getPlayerMail(player, unreadOnly);
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not read the inbox for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title(ui(preferences, UiStrings.MAIL_INBOX_HEADER));

        if (mails.isEmpty()) {
            form.content(ui(preferences, UiStrings.MAIL_NO_MAILS));
        } else {
            // no content line when there are rows, the buttons speak for
            // themselves and Bedrock gives the form little vertical room
            for (PlayerMail mail : mails) {
                // Two lines per button: who it is from, then enough of the
                // message to tell them apart. The full text is one tap away,
                // so this preview is a label rather than the only copy.
                String sender = MailMessagesManager.getMailSenderDisplayName(mail);
                String preview = preview(PlayerPreferencesManager.localizeMessageForPlayer(
                        preferences, mail.message));
                form.button((mail.readAt == null ? "§l" : "") + sender + "\n§7" + preview);
            }
        }

        // Always last, so its index is stable however many mails there are
        form.button(ui(preferences, UiStrings.MAIL_SEND_TITLE));

        UUID playerId = player.getUniqueId();
        List<PlayerMail> shown = new ArrayList<>(mails);
        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == shown.size()) {
                onScheduler(playerId, BedrockMailInbox::showCompose);
            } else if (index >= 0 && index < shown.size()) {
                onScheduler(playerId, target -> showMail(target, shown.get(index)));
            }
        });
        form.closedOrInvalidResultHandler(() -> { });

        return BedrockForms.send(playerId, form);
    }

    /** One mail, in full, with what can be done to it. */
    public static boolean showMail(Player player, PlayerMail mail) {
        if (!isSupported(player) || mail == null) {
            return false;
        }

        PlayerPreferences preferences;
        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(player);
        } catch (Exception e) {
            return false;
        }

        String sender = MailMessagesManager.getMailSenderDisplayName(mail);
        String body = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

        StringBuilder content = new StringBuilder();
        content.append("§7").append(sender).append("\n\n").append(body);
        if (mail.originalLanguage != null && !body.equals(mail.message)) {
            // Bedrock loses the hover that used to carry this, so the
            // original goes in the body where it can actually be read
            content.append("\n\n§8[").append(mail.originalLanguage.toUpperCase()).append("] ")
                    .append(mail.message);
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title(ui(preferences, UiStrings.MAIL_INBOX_HEADER))
                .content(content.toString())
                .button(ui(preferences, UiStrings.MAIL_CLICK_TO_REPLY))
                .button(mail.readAt == null
                        ? ui(preferences, UiStrings.MAIL_MARK_AS_READ)
                        : ui(preferences, UiStrings.MAIL_ALREADY_READ));

        UUID playerId = player.getUniqueId();
        form.validResultHandler(response -> {
            int button = response.clickedButtonId();
            onScheduler(playerId, target -> {
                if (button == 0) {
                    showReply(target, mail);
                } else if (button == 1 && mail.readAt == null) {
                    MailMessagesManager.markMailAsRead(mail);
                    showInbox(target, true);
                }
            });
        });
        form.closedOrInvalidResultHandler(() -> { });

        return BedrockForms.send(playerId, form);
    }

    /**
     * Writing a mail: who, then what, in one form.
     *
     * A dropdown of online players plus a free text field for anyone else,
     * because the proxy can only enumerate who is connected and mail is
     * deliverable to people who are not.
     */
    public static boolean showCompose(Player player) {
        if (!isSupported(player)) {
            return false;
        }

        PlayerPreferences preferences;
        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(player);
        } catch (Exception e) {
            return false;
        }

        List<String> online = new ArrayList<>();
        for (Player other : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                online.add(other.getUsername());
            }
        }
        online.add(ui(preferences, UiStrings.MAIL_SOMEBODY_ELSE));

        CustomForm.Builder form = CustomForm.builder()
                .title(ui(preferences, UiStrings.MAIL_SEND_TITLE))
                .dropdown(ui(preferences, UiStrings.MAIL_RECIPIENT), online)
                .input(ui(preferences, UiStrings.MAIL_RECIPIENT))
                .input(ui(preferences, UiStrings.MAIL_MESSAGE));

        UUID playerId = player.getUniqueId();
        int otherIndex = online.size() - 1;
        form.validResultHandler(response -> {
            Integer chosen = readInt(response, 0);
            String typedName = readString(response, 1);
            String message = readString(response, 2);
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            // The dropdown wins unless they picked the escape hatch, in which
            // case the typed name is the only thing that can be meant
            String recipient = (chosen != null && chosen == otherIndex)
                    ? typedName
                    : (chosen != null && chosen >= 0 && chosen < otherIndex ? online.get(chosen) : typedName);
            if (recipient == null || recipient.trim().isEmpty()) {
                return;
            }
            onScheduler(playerId, target -> deliver(target, recipient.trim(), message.trim()));
        });
        form.closedOrInvalidResultHandler(() -> { });

        return BedrockForms.send(playerId, form);
    }

    private static void deliver(Player player, String recipient, String message) throws Exception {
        PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
        PlayerPreferences target = PlayerPreferencesManager.getPlayerPreferences(recipient, true, false);
        if (target == null) {
            BasicMessageFormatter.sendInternalError(player, "That player was not found");
            return;
        }
        MailMessagesManager.sendMail(player, target.minecraftUuid, message);
        BasicMessageFormatter.sendInternalMessage(player,
                PlayerPreferencesManager.localizeUiForPlayer(preferences, "Mail sent to ") + recipient,
                NamedTextColor.GREEN);
    }

    private static Integer readInt(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
        try {
            Object value = response.valueAt(index);
            return value instanceof Integer ? (Integer) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readString(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
        try {
            Object value = response.valueAt(index);
            return value instanceof String ? (String) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** A text box, which is the one thing chat could never offer here. */
    private static boolean showReply(Player player, PlayerMail mail) {
        PlayerPreferences preferences;
        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(player);
        } catch (Exception e) {
            return false;
        }

        String sender = MailMessagesManager.getMailSenderDisplayName(mail);
        CustomForm.Builder form = CustomForm.builder()
                .title(ui(preferences, UiStrings.MAIL_CLICK_TO_REPLY))
                .label("§7" + sender)
                .input(ui(preferences, UiStrings.MAIL_CLICK_TO_REPLY));

        UUID playerId = player.getUniqueId();
        form.validResultHandler(response -> {
            Object typed = response.valueAt(1);
            String message = typed instanceof String ? ((String) typed).trim() : "";
            if (message.isEmpty()) {
                return;
            }
            onScheduler(playerId, target -> {
                MailMessagesManager.sendMail(target, mail.senderUuid, message);
                BasicMessageFormatter.sendInternalMessage(target,
                        PlayerPreferencesManager.localizeUiForPlayer(preferences, "Mail sent to ") + sender,
                        NamedTextColor.GREEN);
            });
        });
        form.closedOrInvalidResultHandler(() -> { });

        return BedrockForms.send(playerId, form);
    }

    private static String preview(String message) {
        String flat = message == null ? "" : message.replace('\n', ' ');
        return flat.length() <= 40 ? flat : flat.substring(0, 40) + "...";
    }

    private interface PlayerTask {
        void run(Player player) throws Exception;
    }

    private static void onScheduler(UUID playerId, PlayerTask task) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            Player player = plugin.getProxy().getPlayer(playerId).orElse(null);
            if (player == null) {
                return;
            }
            try {
                task.run(player);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Bedrock mail form failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    private static String ui(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }
}
