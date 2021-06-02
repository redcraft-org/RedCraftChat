package org.redcraft.redcraftchat.listeners.discord;

import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.discord.ChannelManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMappingList;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordMessageEditedListener extends ListenerAdapter {

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePublicMessage(MessageUpdateEvent event) {
        WebhookMessageMappingList webhookMessages = DiscordClient.getWebhookMessagesFromOriginalMessage(event.getMessageId());
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (member != null && webhookMessages != null) {
            List<WebhookMessageMapping> postedWebhooks = new ArrayList<WebhookMessageMapping>();

            for (WebhookMessageMapping webhookMessage : webhookMessages.mappingList) {
                TranslatedChannel sourceChannel = new TranslatedChannel(webhookMessage.guildId, event.getMessageId(), webhookMessages.originalLangId);
                try {
                    TextChannel channel = guild.getTextChannelById(webhookMessage.channelId);

                    if (channel != null) {
                        channel.deleteMessageById(webhookMessage.messageId).complete();

                        TranslatedChannel targetChannel = new TranslatedChannel(webhookMessage.guildId, webhookMessage.channelId, webhookMessage.languageId);

                        postedWebhooks.add(
                            ChannelManager.translateAndPublishMessage(sourceChannel, targetChannel, member, event.getMessage(), webhookMessage.messageId)
                        );
                    }
                } catch (Exception e) {
                    String messageTemplate = "Error while handling performing message edit from %s channel %s [%s] from user %s";
                    String warningMessage = String.format(messageTemplate, event.getGuild().getName(), event.getChannel().getName(), sourceChannel.languageId, member.getEffectiveName());
                    RedCraftChat.getInstance().getLogger().warning(warningMessage);
                    e.printStackTrace();

                    // keep original message mapping if we can't replace it
                    postedWebhooks.add(webhookMessage);
                }
            }

            WebhookMessageMappingList postedWebhooksList = new WebhookMessageMappingList(postedWebhooks, webhookMessages.originalLangId);

            CacheManager.put(CacheCategory.WEBHOOK_MESSAGE_MAPPING, event.getMessageId(), postedWebhooksList);
        }
    }
}
