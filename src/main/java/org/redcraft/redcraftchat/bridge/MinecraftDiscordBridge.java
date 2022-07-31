package org.redcraft.redcraftchat.bridge;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vdurmont.emoji.EmojiParser;

import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;
import org.redcraft.redcraftchat.discord.ChannelManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MinecraftDiscordBridge {

    private TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

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

    public void broadcastMessage(String message) {
        broadcastMessage(message, null, null);
    }

    public void broadcastMessage(String message, ProxiedPlayer sender) {
        broadcastMessage(message, null, sender);
    }

    public void broadcastMessage(String message, Map<String, String> replacements) {
        broadcastMessage(message, replacements, null);
    }

    public void broadcastMessage(String message, Map<String, String> replacements, ProxiedPlayer sender) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        Map<String, String> tokens = new HashMap<String, String>();

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                String token = TokenizerManager.generateToken(entry.getKey());
                tokens.put(token, entry.getValue());
                formattedMessage = formattedMessage.replace(entry.getKey(), token);
            }
        }

        for (TranslatedChannel channel : ChannelManager.getMinecraftBridgeChannels()) {
            String targetMessage = formattedMessage;
            String originalLanguage = TranslationManager.getSourceLanguage(formattedMessage, null);

            if (originalLanguage == null && sender != null) {
                originalLanguage = PlayerPreferencesManager.getMainPlayerLanguage(sender);
            }

            if (originalLanguage == null) {
                originalLanguage = "en";
            }

            if (!channel.languageId.equals(originalLanguage)) {
                try {
                    targetMessage = translationManager.translate(targetMessage, originalLanguage, channel.languageId);
                } catch (IllegalStateException | URISyntaxException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            targetMessage = TokenizerManager.replaceTokens(targetMessage, tokens);

            if (channel.languageId.equals("en")) {
                RedCraftChat.getInstance().getLogger().info("Broadcasting message: " + targetMessage);
            }

            TextComponent parsedMessage = LegacyComponentSerializer.legacySection().deserialize(targetMessage);
            String discordMessage = DiscordSerializer.INSTANCE.serialize(parsedMessage);

            if (sender != null) {
                DiscordClient.postAsPlayer(channel.channelId, sender, discordMessage, "");
            } else {
                TextChannel discordChannel = DiscordClient.getClient().getTextChannelById(channel.channelId);
                discordChannel.sendMessage(discordMessage).queue();
            }
        }

        for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
            String targetMessage = PlayerPreferencesManager.localizeMessageForPlayer(receiver, formattedMessage);

            targetMessage = TokenizerManager.replaceTokens(targetMessage, tokens);

            String originalMessage = TokenizerManager.replaceTokens(formattedMessage, tokens);

            receiver.sendMessage(new ComponentBuilder(targetMessage)
                    .event(new HoverEvent(Action.SHOW_TEXT, new Text(
                            originalMessage)))
                    .create());
        }
    }

    public void sendMessageToDiscord(String server, ProxiedPlayer sender, String sourceLanguage, String originalMessage, Map<String, String> translatedLanguages) {
        for (TranslatedChannel channel : ChannelManager.getMinecraftBridgeChannels()) {
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
