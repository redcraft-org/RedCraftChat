package org.redcraft.redcraftchat.listeners.discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageReceivedListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            this.handlePrivateMessage(event);
        } else if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        event.getChannel().sendMessage("Sorry, I can't handle private messages yet!");
    }

    public void handlePublicMessage(MessageReceivedEvent event) {
        // Ignore messages coming from bots
        if (event.getMember() == null) {
            return;
        }

        HashMap<TranslatedChannel, List<TranslatedChannel>> translatedChannelsMappings = ChannelManager.getTranslatedChannelsMapping();

        String channelId = event.getChannel().getId();
        TranslatedChannel sourceChannel = null;

        for (TranslatedChannel potentialChannel : translatedChannelsMappings.keySet()) {
            if (potentialChannel.channelId.equals(channelId)) {
                sourceChannel = potentialChannel;
                break;
            }
        }

        if (translatedChannelsMappings.containsKey(sourceChannel)) {
            try {
                String originalMessage = event.getMessage().getContentRaw();

                List<WebhookAsUser> webhooksToPost = new ArrayList<WebhookAsUser>();

                Member member = event.getMember();
                List<Attachment> attachments = event.getMessage().getAttachments();

                List<TranslatedChannel> translatedChannels = translatedChannelsMappings.get(sourceChannel);

                for (TranslatedChannel translatedChannel : translatedChannels) {
                    Guild guild = DiscordClient.getClient().getGuildById(translatedChannel.guildId);

                    String translatedMessage = TranslationManager.translate(originalMessage, sourceChannel.languageId, translatedChannel.languageId);
                    TextChannel responseChannel = guild.getTextChannelById(translatedChannel.channelId);

                    WebhookAsUser webhookToPost = new WebhookAsUser(responseChannel, member, translatedMessage, attachments);
                    webhooksToPost.add(webhookToPost);
                }

                List<WebhookMessageMapping> webhookMessagesMapping = new ArrayList<WebhookMessageMapping>();

				for (WebhookAsUser webhookToPost: webhooksToPost) {
                    ReadonlyMessage webhookMessage = DiscordClient.postAsUser(webhookToPost);
                    String webhookMessageId = String.valueOf(webhookMessage.getId());

                    String guildId = event.getGuild().getId();
                    String sourceChannelId = event.getTextChannel().getId();
                    String targetChannelId = String.valueOf(webhookMessage.getChannelId());

                    WebhookMessageMapping webhookMessageMapping = new WebhookMessageMapping(guildId, targetChannelId, webhookMessageId);
                    UserMessageMapping userMessageMapping = new UserMessageMapping(guildId, sourceChannelId, event.getMessageId());

                    webhookMessagesMapping.add(webhookMessageMapping);
                    CacheManager.put(CacheCategory.USER_MESSAGE_MAPPING, webhookMessageId, userMessageMapping);
                }

                WebhookMessageMappingList webhookMessagesMappingList = new WebhookMessageMappingList(webhookMessagesMapping);

                CacheManager.put(CacheCategory.WEBHOOK_MESSAGE_MAPPING, event.getMessageId(), webhookMessagesMappingList);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
