package org.redcraft.redcraftchat.listeners.discord;

import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMappingList;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordMessageDeletedListener extends ListenerAdapter {

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePublicMessage(MessageDeleteEvent event) {
        WebhookMessageMappingList webhookMessages = DiscordClient.getWebhookMessagesFromOriginalMessage(event.getMessageId());

        if (webhookMessages != null) {
            for (WebhookMessageMapping webhookMessage: webhookMessages.mappingList) {
             DiscordClient.getClient().getTextChannelById(webhookMessage.channelId).deleteMessageById(webhookMessage.messageId).complete();
            }
        }
    }
}
