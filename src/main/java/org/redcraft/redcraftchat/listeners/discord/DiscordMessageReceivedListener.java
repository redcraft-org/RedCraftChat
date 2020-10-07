package org.redcraft.redcraftchat.listeners.discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.discord.ChannelManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.models.discord.UserMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookAsUser;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMappingList;
import org.redcraft.redcraftchat.translate.TranslationManager;

import club.minnced.discord.webhook.receive.ReadonlyMessage;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordMessageReceivedListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
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
        if (event.getMember() == null) {
            return;
        }

        HashMap<TranslatedChannel, List<TranslatedChannel>> translatedChannelsMappings = ChannelManager.getTranslatedChannelsMapping();

        TranslatedChannel sourceChannel = this.getTranslatedChannelFromId(translatedChannelsMappings, event.getChannel().getId());

        if (sourceChannel != null && translatedChannelsMappings.containsKey(sourceChannel)) {
            Message message = event.getMessage();
            Member member = event.getMember();

            try {
                List<TranslatedChannel> targetChannels = translatedChannelsMappings.get(sourceChannel);

                List<WebhookMessageMapping> postedWebhooks = new ArrayList<WebhookMessageMapping>();

                for (TranslatedChannel targetChannel : targetChannels) {
                    postedWebhooks.add(
                        this.translateAndPublishMessage(sourceChannel, targetChannel, member, message)
                    );
                }

                WebhookMessageMappingList postedWebhooksList = new WebhookMessageMappingList(postedWebhooks);

                CacheManager.put(CacheCategory.WEBHOOK_MESSAGE_MAPPING, event.getMessageId(), postedWebhooksList);
            } catch (Exception e) {
                String messageTemplate = "Error while handling incoming message from server %s channel %s [%s] from user %s";
                String errorMessage = String.format(messageTemplate, event.getGuild().getName(), event.getChannel().getName(), sourceChannel.languageId, member.getEffectiveName());
                RedCraftChat.getInstance().getLogger().severe(errorMessage);
                e.printStackTrace();
            }
        }
    }

    private TranslatedChannel getTranslatedChannelFromId(HashMap<TranslatedChannel, List<TranslatedChannel>> translatedChannelsMappings, String channelId) {
        for (TranslatedChannel channel : translatedChannelsMappings.keySet()) {
            if (channel.channelId.equals(channelId)) {
                return channel;
            }
        }

        return null;
    }

    private WebhookMessageMapping translateAndPublishMessage(TranslatedChannel sourceChannel, TranslatedChannel targetChannel, Member member, Message message) throws Exception {
        Guild guild = DiscordClient.getClient().getGuildById(sourceChannel.guildId);

        String translatedMessage = TranslationManager.translate(message.getContentRaw(), sourceChannel.languageId, targetChannel.languageId);
        TextChannel responseChannel = guild.getTextChannelById(targetChannel.channelId);

        WebhookAsUser webhookToPost = new WebhookAsUser(responseChannel, member, translatedMessage, message.getAttachments());

        ReadonlyMessage webhookMessage = DiscordClient.postAsUser(webhookToPost);
        String webhookMessageId = String.valueOf(webhookMessage.getId());

        WebhookMessageMapping webhookMessageMapping = new WebhookMessageMapping(sourceChannel.guildId, targetChannel.channelId, webhookMessageId);
        UserMessageMapping userMessageMapping = new UserMessageMapping(sourceChannel.guildId, sourceChannel.channelId, message.getId());

        CacheManager.put(CacheCategory.USER_MESSAGE_MAPPING, webhookMessageId, userMessageMapping);

        return webhookMessageMapping;
    }
}
