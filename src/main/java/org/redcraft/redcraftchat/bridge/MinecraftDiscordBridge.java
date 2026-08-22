package org.redcraft.redcraftchat.bridge;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vdurmont.emoji.EmojiParser;
import com.velocitypowered.api.proxy.Player;

import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;
import org.redcraft.redcraftchat.discord.ChannelManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MinecraftDiscordBridge {

    private TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

    public class AsyncMinecraftMessageTranslator implements Runnable {
        TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

        Player sender;
        String message;

        AsyncMinecraftMessageTranslator(Player sender, String message) {
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

            String server = getServerName(sender);

            // Send to players
            MinecraftDiscordBridge.getInstance().sendMessageToPlayers(server, DisplayNameManager.getDisplayName(sender), sourceLanguage, message, translatedLanguages);

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

    public static String getServerName(Player player) {
        return player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("unknown");
    }

    public void broadcastMessage(String message) {
        broadcastMessage(message, null, null);
    }

    public void broadcastMessage(String message, Player sender) {
        broadcastMessage(message, null, sender);
    }

    public void broadcastMessage(String message, Map<String, String> replacements) {
        broadcastMessage(message, replacements, null);
    }

    public void broadcastMessage(String message, Map<String, String> replacements, Player sender) {
        String formattedMessage = LegacyText.translateAlternateColorCodes('&', message);

        Map<String, String> tokens = new HashMap<String, String>();

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                String token = TokenizerManager.generateToken(entry.getKey());
                tokens.put(token, entry.getValue());
                formattedMessage = formattedMessage.replace(entry.getKey(), token);
            }
        }

        broadcastDiscord(formattedMessage, tokens, sender);
        broadcastMinecraft(formattedMessage, tokens);
    }

    public void broadcastDiscord(String formattedMessage, Map<String, String> tokens, Player sender) {
        if (!DiscordClient.hasClient()) {
            return;
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
                } catch (IllegalStateException | URISyntaxException | IOException | InterruptedException e) {
                    RedCraftChat.getInstance().getLogger().error("Failed to translate message from " + originalLanguage + " to " + channel.languageId);
                    e.printStackTrace();
                }
            }

            targetMessage = TokenizerManager.replaceTokens(targetMessage, tokens);

            TextComponent parsedMessage = LegacyComponentSerializer.legacySection().deserialize(targetMessage);
            String discordMessage = DiscordSerializer.INSTANCE.serialize(parsedMessage);

            if (sender != null) {
                DiscordClient.postAsPlayer(channel.channelId, sender, discordMessage, "[" + getServerName(sender) + "]");
            } else {
                TextChannel discordChannel = DiscordClient.getClient().getTextChannelById(channel.channelId);
                discordChannel.sendMessage(discordMessage).queue();
            }
        }
    }

    public void broadcastMinecraft(String formattedMessage, Map<String, String> tokens) {
        for (Player receiver : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            String targetMessage = PlayerPreferencesManager.localizeMessageForPlayer(receiver, formattedMessage, Config.chatTranslationProvider);

            targetMessage = TokenizerManager.replaceTokens(targetMessage, tokens);

            String originalMessage = TokenizerManager.replaceTokens(formattedMessage, tokens);

            receiver.sendMessage(LegacyComponentSerializer.legacySection().deserialize(targetMessage)
                    .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(originalMessage))));
        }
    }

    public void sendMessageToDiscord(String server, Player sender, String sourceLanguage, String originalMessage, Map<String, String> translatedLanguages) {
        if (!DiscordClient.hasClient()) {
            return;
        }

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
        for (Player receiver : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            String targetLanguage = sourceLanguage;
            if (!PlayerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
                targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(receiver).toLowerCase();
            }
            String translatedMessage = translatedLanguages.get(targetLanguage);
            if (translatedMessage == null) {
                translatedMessage = originalMessage;
            }
            formatAndSendMessageToPlayer(server, sender, receiver, translatedMessage, originalMessage, sourceLanguage);
        }
    }

    public void formatAndSendMessageToPlayer(String server, String sender, Player receiver, String translatedMessage, String originalMessage, String sourceLanguage) {
        String serverPrefix = server + LegacyText.RESET;
        String senderPrefix = sender + LegacyText.RESET;

        String targetLanguage = null;
        if (!PlayerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
            targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(receiver);
        }

        String languagePrefix = TranslationManager.getLanguagePrefix(sourceLanguage, targetLanguage);

        String parsedTranslatedMessage = EmojiParser.parseToAliases(translatedMessage);

        String formattedMessage = "[" + languagePrefix + "]"
                + "[" + serverPrefix + "]"
                + "[" + senderPrefix + "] "
                + parsedTranslatedMessage;

        Component message = LegacyComponentSerializer.legacySection().deserialize(formattedMessage);

        if (targetLanguage != null) {
            message = message.hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(originalMessage)));
        }

        receiver.sendMessage(message);
    }

    public void translateAndPostMessage(Player sender, String message) {
        RedCraftChat pluginInstance = RedCraftChat.getInstance();
        AsyncMinecraftMessageTranslator minecraftMessageTranslator = new AsyncMinecraftMessageTranslator(sender, message);

        pluginInstance.getProxy().getScheduler().buildTask(pluginInstance, minecraftMessageTranslator).schedule();
    }
}
