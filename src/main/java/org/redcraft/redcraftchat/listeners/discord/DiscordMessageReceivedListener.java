package org.redcraft.redcraftchat.listeners.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.discord.ChannelManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMappingList;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordMessageReceivedListener extends ListenerAdapter {

    TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore commands
        if (event.getMessage().getContentRaw().equals("") || event.getMessage().getContentRaw().startsWith("/")) {
            return;
        }

        if (event.isFromType(ChannelType.PRIVATE)) {
            this.handlePrivateMessage(event);
        } else if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        event.getChannel().sendMessage("Sorry, I don't handle private messages yet!");
    }

    public void handlePublicMessage(MessageReceivedEvent event) {
        // Ignore messages coming from bots
        if (event.getMember() == null || event.getAuthor().isBot()) {
            return;
        }

        Map<TranslatedChannel, List<TranslatedChannel>> translatedChannelsMappings = ChannelManager.getTranslatedChannelsMapping();

        Message message = event.getMessage();
        Member member = event.getMember();

        TranslatedChannel sourceChannel = this.getTranslatedChannelFromId(translatedChannelsMappings, event.getChannel().getId());

        if (sourceChannel != null && ChannelManager.getMinecraftBridgeChannels().contains(sourceChannel)) {
            List<String> targetLanguages = TranslationManager.getTargetLanguages(sourceChannel.languageId);
            Map<String, String> translatedLanguages = translationManager.translateBulk(message.getContentDisplay(), sourceChannel.languageId, targetLanguages);
            MinecraftDiscordBridge.getInstance().sendMessageToPlayers("Discord", member.getEffectiveName(), sourceChannel.languageId, message.getContentDisplay(), translatedLanguages);
        }

        if (sourceChannel != null && translatedChannelsMappings.containsKey(sourceChannel)) {
            try {
                List<TranslatedChannel> targetChannels = translatedChannelsMappings.get(sourceChannel);

                List<WebhookMessageMapping> postedWebhooks = new ArrayList<WebhookMessageMapping>();

                for (TranslatedChannel targetChannel : targetChannels) {
                    postedWebhooks.add(
                        ChannelManager.translateAndPublishMessage(sourceChannel, targetChannel, member, message, null)
                    );
                }

                WebhookMessageMappingList postedWebhooksList = new WebhookMessageMappingList(postedWebhooks, sourceChannel.languageId);

                CacheManager.put(CacheCategory.WEBHOOK_MESSAGE_MAPPING, event.getMessageId(), postedWebhooksList);
            } catch (Exception e) {
                String messageTemplate = "Error while handling incoming message from server %s channel %s [%s] from user %s";
                String errorMessage = String.format(messageTemplate, event.getGuild().getName(), event.getChannel().getName(), sourceChannel.languageId, member.getEffectiveName());
                RedCraftChat.getInstance().getLogger().severe(errorMessage);
                e.printStackTrace();
            }
        }
    }

    private TranslatedChannel getTranslatedChannelFromId(Map<TranslatedChannel, List<TranslatedChannel>> translatedChannelsMappings, String channelId) {
        for (TranslatedChannel channel : translatedChannelsMappings.keySet()) {
            if (channel.channelId.equals(channelId)) {
                return channel;
            }
        }

        return null;
    }
}
