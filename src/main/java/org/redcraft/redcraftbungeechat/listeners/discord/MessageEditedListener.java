package org.redcraft.redcraftbungeechat.listeners.discord;

import org.redcraft.redcraftbungeechat.RedCraftBungeeChat;
import org.redcraft.redcraftbungeechat.caching.CacheManager;
import org.redcraft.redcraftbungeechat.models.caching.CacheCategory;
import org.redcraft.redcraftbungeechat.models.discord.WebhookMessageMapping;

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
        if (event.getMember() == null) {
            return;
        }

        WebhookMessageMapping webhook = (WebhookMessageMapping) CacheManager.get(CacheCategory.WEBHOOK_MESSAGE_MAPPING, event.getMessageId(), WebhookMessageMapping.class);

        String messageTemplate = "Got an edit on %s that should have edited %s but can't edit because of this https://support.discord.com/hc/en-us/community/posts/360034557771";
        String debugMessage = String.format(messageTemplate, event.getMessageId(), webhook.messageId);

        RedCraftBungeeChat.getInstance().getLogger().info(debugMessage);
    }
}
