package org.redcraft.redcraftbungeechat.listeners.discord;

import org.redcraft.redcraftbungeechat.caching.CacheManager;
import org.redcraft.redcraftbungeechat.discord.DiscordClient;
import org.redcraft.redcraftbungeechat.models.caching.CacheCategory;
import org.redcraft.redcraftbungeechat.models.discord.WebhookMessageMapping;

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
