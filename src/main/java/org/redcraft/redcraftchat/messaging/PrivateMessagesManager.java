package org.redcraft.redcraftchat.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.translate.TranslationManager;

import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class PrivateMessagesManager {

    private static TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

    private PrivateMessagesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void handlePrivateMessage(Player sender, Player receiver, String message) {
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
                RedCraftChat.getInstance().getLogger().warn("Failed to translate message: " + e.getMessage());
            }
        }

        // Send message to players
        String senderDisplayName = DisplayNameManager.getDisplayName(sender);
        String receiverDisplayName = DisplayNameManager.getDisplayName(receiver);

        sendToPlayer(receiver, senderDisplayName, receiverDisplayName, translatedMessage, message, messageLanguage, targetLanguage, sender.getUsername());
        CacheManager.put(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, receiver.getUniqueId().toString(), sender.getUniqueId());

        // Make sure we're not sending duplicate if you send a message to yourself
        if (!sender.getUniqueId().equals(receiver.getUniqueId())) {
            sendToPlayer(sender, senderDisplayName, receiverDisplayName, message, translatedMessage, messageLanguage, targetLanguage, receiver.getUsername());
            CacheManager.put(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, sender.getUniqueId().toString(), receiver.getUniqueId());
        }
    }

    public static void sendToPlayer(Player player, String senderDisplayName, String receiverDisplayName, String displayedMessage, String hoverMessage, String originalLanguage, String targetLanguage, String replyTo) {
        String languagePrefix = TranslationManager.getLanguagePrefix(originalLanguage, targetLanguage);

        TextComponent.Builder messageBuilder = Component.text()
                .append(BasicMessageFormatter.deserialize("[" + LegacyText.LIGHT_PURPLE + LegacyText.BOLD + "PM" + LegacyText.RESET + "]"));

        if (languagePrefix != null) {
            messageBuilder.append(Component.text("[" + languagePrefix + "]"));
        }

        messageBuilder.append(Component.text("["));

        String sender = BasicMessageFormatter.getDisplayNameWithoutRank(senderDisplayName);
        String receiver = BasicMessageFormatter.getDisplayNameWithoutRank(receiverDisplayName);

        Component senderComponent = BasicMessageFormatter.deserialize(sender + LegacyText.RESET);
        if (!sender.equals(senderDisplayName)) {
            senderComponent = senderComponent.hoverEvent(HoverEvent.showText(BasicMessageFormatter.deserialize(senderDisplayName)));
        }
        messageBuilder.append(senderComponent);
        messageBuilder.append(Component.text(" ➔ "));

        Component receiverComponent = BasicMessageFormatter.deserialize(receiver + LegacyText.RESET);
        if (!receiver.equals(receiverDisplayName)) {
            receiverComponent = receiverComponent.hoverEvent(HoverEvent.showText(BasicMessageFormatter.deserialize(receiverDisplayName)));
        }
        messageBuilder.append(receiverComponent);

        messageBuilder.append(Component.text("] "));

        Component messageComponent = BasicMessageFormatter.deserialize(displayedMessage);

        List<String> tooltip = new ArrayList<String>();

        if (hoverMessage != null && !hoverMessage.equals(displayedMessage)) {
             tooltip.add(hoverMessage);
        }

        if (replyTo != null) {
            messageComponent = messageComponent.clickEvent(ClickEvent.suggestCommand("/msg " + replyTo + " "));
            tooltip.add(LegacyText.DARK_AQUA + PlayerPreferencesManager.localizeUiForPlayer(player, "Click to reply"));
        }

        if (!tooltip.isEmpty()) {
            messageComponent = messageComponent.hoverEvent(HoverEvent.showText(BasicMessageFormatter.deserialize(String.join("\n", tooltip))));
        }

        messageBuilder.append(messageComponent);

        player.sendMessage(messageBuilder.build());
    }

    // Returns false if no we cannot find who to reply to
    public static boolean handleReply(Player sender, String message) {
        UUID lastSender = (UUID) CacheManager.get(CacheCategory.LAST_PRIVATE_MESSAGE_SENDER, sender.getUniqueId().toString(), UUID.class);

        if (lastSender == null) {
            return false;
        }

        Player lastSenderPlayer = RedCraftChat.getInstance().getProxy().getPlayer(lastSender).orElse(null);
        if (lastSenderPlayer == null) {
            return false;
        }

        handlePrivateMessage(sender, lastSenderPlayer, message);
        return true;
    }
}
