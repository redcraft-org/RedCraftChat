package org.redcraft.redcraftchat.messaging;

import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PrivateMessagesManager {

    private static TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

    public PrivateMessagesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void handlePrivateMessage(ProxiedPlayer sender, ProxiedPlayer receiver, String message) {
        // Get message language
        String messageLanguage = DetectionManager.getLanguage(message);
        if (messageLanguage == null) {
            messageLanguage = PlayerPreferencesManager.getMainPlayerLanguage(sender);
        }

        // Get target language
        String targetLanguage = messageLanguage;
        if (!PlayerPreferencesManager.playerSpeaksLanguage(receiver, message)) {
            targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(receiver);
        }

        // Translate message
        String translatedMessage = message;
        if (!messageLanguage.equals(targetLanguage)) {
            try {
                translatedMessage = translationManager.translate(translatedMessage, messageLanguage, targetLanguage);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warning("Failed to translate message: " + e.getMessage());
            }
        }

        // Send message to players
        String senderDisplayName = sender.getDisplayName();
        String receiverDisplayName = receiver.getDisplayName();

        sendToPlayer(sender, senderDisplayName, receiverDisplayName, translatedMessage, message, messageLanguage, targetLanguage, receiver.getName());
        sendToPlayer(receiver, senderDisplayName, receiverDisplayName, message, translatedMessage, messageLanguage, targetLanguage, sender.getName());

        // Set last private message sender
        CacheManager.put(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, sender.getUniqueId().toString(), receiver.getUniqueId());
        CacheManager.put(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, receiver.getUniqueId().toString(), sender.getUniqueId());
    }

    public static void sendToPlayer(ProxiedPlayer player, String senderDisplayName, String receiverDisplayName, String displayedMessage, String hoverMessage, String originalLanguage, String targetLanguage, String replyTo) {
        String languagePrefix = TranslationManager.getLanguagePrefix(originalLanguage, targetLanguage);

        String messagePrefix = "[" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + PlayerPreferencesManager.localizeMessageForPlayer(player, "Private message") + ChatColor.RESET + "]";

        if (languagePrefix != null) {
            messagePrefix += "[" + languagePrefix + "]";
        }

        messagePrefix += "[" + senderDisplayName + ChatColor.RESET + " ➔ " + receiverDisplayName + ChatColor.RESET +  "] ";

        ComponentBuilder messageBuilder = new ComponentBuilder(messagePrefix);

        if (replyTo != null) {
                messageBuilder.event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/msg " + receiverDisplayName + " "));
                String clickToReply = PlayerPreferencesManager.localizeMessageForPlayer(player, "Click to reply");
                messageBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.DARK_AQUA + clickToReply)));
        }

        messageBuilder.append(displayedMessage);

        if (hoverMessage != null && !hoverMessage.equals(displayedMessage)) {
             messageBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(hoverMessage)));
        }

        player.sendMessage(messageBuilder.create());
    }

    // Returns false if no we cannot find who to reply to
    public static boolean handleReply(ProxiedPlayer sender, String message) {
        UUID lastSender = (UUID) CacheManager.get(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, sender.getUniqueId().toString(), UUID.class);

        if (lastSender == null) {
            return false;
        }

        ProxiedPlayer lastSenderPlayer = RedCraftChat.getInstance().getProxy().getPlayer(lastSender);
        if (lastSenderPlayer == null) {
            return false;
        }

        handlePrivateMessage(sender, lastSenderPlayer, message);
        return true;
    }
}
