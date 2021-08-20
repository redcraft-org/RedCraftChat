package org.redcraft.redcraftchat.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.database.PlayerPreferencesManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MinecraftDiscordBridge {

    public class AsyncMinecraftMessageTranslator implements Runnable {
        ProxiedPlayer sender;
        String message;

        AsyncMinecraftMessageTranslator(ProxiedPlayer sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        @Override
        public void run() {
            String sourceLanguage = DetectionManager.getLanguage(message);

            if (sourceLanguage == null) {
                sourceLanguage = playerPreferencesManager.getMainPlayerLanguage(sender);
            }

            // Gather languages
            List<String> targetLanguages = new ArrayList<String>(Config.translationSupportedLanguages);

            for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
                if (!playerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
                    String playerLanguage = playerPreferencesManager.getMainPlayerLanguage(receiver).toLowerCase();
                    if (!targetLanguages.contains(playerLanguage)) {
                        targetLanguages.add(playerLanguage);
                    }
                }
            }

            // Translate
            Map<String, String> translatedLanguages = TranslationManager.translateBulk(message, sourceLanguage, targetLanguages);

            // Send to players
            for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
                String targetLanguage = sourceLanguage;
                if (!playerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
                    targetLanguage = playerPreferencesManager.getMainPlayerLanguage(receiver).toLowerCase();
                }
                String translatedMessage = translatedLanguages.get(targetLanguage);
                if (translatedMessage == null) {
                    translatedMessage = message;
                }
                MinecraftDiscordBridge.getInstance().formatAndSendMessageToPlayer(sender, receiver, translatedMessage, sourceLanguage);
            }

            // Send to Discord
            // TODO
        }
    }

    private PlayerPreferencesManager playerPreferencesManager = new PlayerPreferencesManager();

    private static MinecraftDiscordBridge instance = null;

    public static MinecraftDiscordBridge getInstance() {
        if (instance == null) {
            instance = new MinecraftDiscordBridge();
        }

        return instance;
    }

    public void formatAndSendMessageToPlayer(ProxiedPlayer sender, ProxiedPlayer receiver, String translatedMessage, String sourceLanguage) {
        String languagePrefix = sourceLanguage;
        String serverPrefix = sender.getServer().getInfo().getName() + ChatColor.RESET;
        String senderPrefix = sender.getDisplayName() + ChatColor.RESET;

        if (!playerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
            String targetLanguage = playerPreferencesManager.getMainPlayerLanguage(receiver);
            if (!targetLanguage.equalsIgnoreCase(sourceLanguage)) {
                try {
                    languagePrefix = sourceLanguage + "->" + targetLanguage;
                } catch (Exception e) {
                    languagePrefix = sourceLanguage + " (translation error)";
                    e.printStackTrace();
                }
            }
        }

        BaseComponent[] formattedMessage = new ComponentBuilder(
                "[" + languagePrefix.toUpperCase() + "][" + serverPrefix + "][" + senderPrefix + "] " + translatedMessage)
                        .create();

        receiver.sendMessage(formattedMessage);
    }

    public void translateAndPostMessage(ProxiedPlayer sender, String message) {
        RedCraftChat pluginInstance = RedCraftChat.getInstance();
        AsyncMinecraftMessageTranslator minecraftMessageTranslator = new AsyncMinecraftMessageTranslator(sender, message);

        pluginInstance.getProxy().getScheduler().runAsync(pluginInstance, minecraftMessageTranslator);
    }
}
