package org.redcraft.redcraftchat.bridge;

import java.util.List;
import java.util.Map;

import com.vdurmont.emoji.EmojiParser;

import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.discord.ChannelManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MinecraftDiscordBridge {

    public class AsyncMinecraftMessageTranslator implements Runnable {
        TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

        ProxiedPlayer sender;
        String message;

        AsyncMinecraftMessageTranslator(ProxiedPlayer sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        @Override
        public void run() {
            // Detect source language
            String sourceLanguage = TranslationManager.getSourceLanguage(message, sender);

            // Gather languages
            List<String> targetLanguages = TranslationManager.getTargetLanguages(sourceLanguage);

            // Translate
            Map<String, String> translatedLanguages = translationManager.translateBulk(message, sourceLanguage, targetLanguages);

            String server = sender.getServer().getInfo().getName();

            // Send to players
            MinecraftDiscordBridge.getInstance().sendMessageToPlayers(server, sender.getDisplayName(), sourceLanguage, message, translatedLanguages);

            // Send to Discord
            MinecraftDiscordBridge.getInstance().sendMessageToDiscord(server, sender, sourceLanguage, message, translatedLanguages);
        }
    }

    private static MinecraftDiscordBridge instance = null;

    public static MinecraftDiscordBridge getInstance() {
        if (instance == null) {
            instance = new MinecraftDiscordBridge();
        }

        return instance;
    }

    public void sendMessageToDiscord(String server, ProxiedPlayer sender, String sourceLanguage, String originalMessage, Map<String, String> translatedLanguages) {
        List<TranslatedChannel> channels = ChannelManager.getMinecraftBridgeChannels();

        for (TranslatedChannel channel : channels) {
            String translatedMessage = translatedLanguages.get(channel.languageId);
            if (translatedMessage == null) {
                translatedMessage = originalMessage;
            }

            String suffix = " [" + TranslationManager.getLanguagePrefix(sourceLanguage, channel.languageId) + "][" + server + "]";

            TextComponent parsedMessage = LegacyComponentSerializer.legacySection().deserialize(translatedMessage);
            String discordMessage = DiscordSerializer.INSTANCE.serialize(parsedMessage);

            DiscordClient.postAsPlayer(channel.channelId, sender, discordMessage, suffix);
        }
    }

    public void sendMessageToPlayers(String server, String sender, String sourceLanguage, String originalMessage, Map<String, String> translatedLanguages) {
        for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
            String targetLanguage = sourceLanguage;
            if (!PlayerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
                targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(receiver).toLowerCase();
            }
            String translatedMessage = translatedLanguages.get(targetLanguage);
            if (translatedMessage == null) {
                translatedMessage = originalMessage;
            }
            formatAndSendMessageToPlayer(server, sender, receiver, translatedMessage, sourceLanguage);
        }
    }

    public void formatAndSendMessageToPlayer(String server, String sender, ProxiedPlayer receiver, String translatedMessage, String sourceLanguage) {
        String languagePrefix = sourceLanguage;
        String serverPrefix = server + ChatColor.RESET;
        String senderPrefix = sender + ChatColor.RESET;

        if (!PlayerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
            String targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(receiver);
            languagePrefix = TranslationManager.getLanguagePrefix(sourceLanguage, targetLanguage);
        }

        String parsedTranslatedMessage = EmojiParser.parseToAliases(translatedMessage);

        BaseComponent[] formattedMessage = new ComponentBuilder(
                "[" + languagePrefix.toUpperCase() + "][" + serverPrefix + "][" + senderPrefix + "] " + parsedTranslatedMessage)
                        .create();

        receiver.sendMessage(formattedMessage);
    }

    public void translateAndPostMessage(ProxiedPlayer sender, String message) {
        RedCraftChat pluginInstance = RedCraftChat.getInstance();
        AsyncMinecraftMessageTranslator minecraftMessageTranslator = new AsyncMinecraftMessageTranslator(sender, message);

        pluginInstance.getProxy().getScheduler().runAsync(pluginInstance, minecraftMessageTranslator);
    }
}
