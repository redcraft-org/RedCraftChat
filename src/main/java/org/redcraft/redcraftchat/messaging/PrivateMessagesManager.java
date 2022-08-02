package org.redcraft.redcraftchat.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
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

        sendToPlayer(receiver, senderDisplayName, receiverDisplayName, translatedMessage, message, messageLanguage, targetLanguage, sender.getName());
        CacheManager.put(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, receiver.getUniqueId().toString(), sender.getUniqueId());

        // Make sure we're not sending duplicate if you send a message to yourself
        if (!sender.getUniqueId().equals(receiver.getUniqueId())) {
            sendToPlayer(sender, senderDisplayName, receiverDisplayName, message, translatedMessage, messageLanguage, targetLanguage, receiver.getName());
            CacheManager.put(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, sender.getUniqueId().toString(), receiver.getUniqueId());
        }
    }

    public static void sendToPlayer(ProxiedPlayer player, String senderDisplayName, String receiverDisplayName, String displayedMessage, String hoverMessage, String originalLanguage, String targetLanguage, String replyTo) {
        String languagePrefix = TranslationManager.getLanguagePrefix(originalLanguage, targetLanguage);

        ComponentBuilder messageBuilder = new ComponentBuilder("[" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "PM" + ChatColor.RESET + "]");

        if (languagePrefix != null) {
            messageBuilder.append("[" + languagePrefix + "]");
        }

        messageBuilder.append("[");

        String sender = BasicMessageFormatter.getDisplayNameWithoutRank(senderDisplayName);
        String receiver = BasicMessageFormatter.getDisplayNameWithoutRank(receiverDisplayName);

        messageBuilder.append(sender + ChatColor.RESET);
        if (!sender.equals(senderDisplayName)) {
            messageBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(senderDisplayName)));
        }
        messageBuilder.append(" ➔ ");

        messageBuilder.append(receiver + ChatColor.RESET);
        if (!receiver.equals(receiverDisplayName)) {
            messageBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(receiverDisplayName)));
        }

        messageBuilder.append("] ");
        messageBuilder.append(displayedMessage);

        List<String> tooltip = new ArrayList<String>();

        if (hoverMessage != null && !hoverMessage.equals(displayedMessage)) {
             tooltip.add(hoverMessage);
        }

        if (replyTo != null) {
            messageBuilder.event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/msg " + replyTo + " "));
            tooltip.add(ChatColor.DARK_AQUA + PlayerPreferencesManager.localizeMessageForPlayer(player, "Click to reply"));
        }

        if (tooltip.size() > 0) {
            messageBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(String.join("\n", tooltip))));
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
