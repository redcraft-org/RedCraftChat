package org.redcraft.redcraftchat.models.discord;

import org.redcraft.redcraftchat.models.SerializableModel;

public class WebhookMessageMapping extends SerializableModel {
    public String guildId;
    public String channelId;
    public String messageId;

    public WebhookMessageMapping(String guildId, String channelId, String messageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
    }
}
