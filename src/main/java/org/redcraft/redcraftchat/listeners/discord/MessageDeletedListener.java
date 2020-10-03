package org.redcraft.redcraftchat.listeners.discord;

import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageDeletedListener extends ListenerAdapter {

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePublicMessage(MessageDeleteEvent event) {
        WebhookMessageMapping webhookMessage = (WebhookMessageMapping) CacheManager.get(CacheCategory.WEBHOOK_MESSAGE_MAPPING, event.getMessageId(), WebhookMessageMapping.class);

        if (webhookMessage != null) {
            DiscordClient.getClient().getTextChannelById(webhookMessage.channelId).deleteMessageById(webhookMessage.messageId).complete();
        }
    }
}
