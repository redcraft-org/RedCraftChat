package org.redcraft.redcraftchat.listeners.discord;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMappingList;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageEditedListener extends ListenerAdapter {

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePublicMessage(MessageUpdateEvent event) {
        WebhookMessageMappingList webhookMessages = DiscordClient.getWebhookMessagesFromOriginalMessage(event.getMessageId());

        if (webhookMessages != null) {
            for (WebhookMessageMapping webhookMessage: webhookMessages.mappingList) {
                String messageTemplate = "Got an edit on %s that should have edited %s but can't edit because of this https://support.discord.com/hc/en-us/community/posts/360034557771";
                String debugMessage = String.format(messageTemplate, event.getMessageId(), webhookMessage.messageId);

                RedCraftChat.getInstance().getLogger().info(debugMessage);
            }
        }
    }
}
