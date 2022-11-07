package org.redcraft.redcraftchat.models.discord;

import org.redcraft.redcraftchat.models.SerializableModel;

public class WebhookMessageMapping extends SerializableModel {
    public String guildId;
    public String channelId;
    public String messageId;
    public String languageId;
    public String authorId;

    public WebhookMessageMapping(String guildId, String channelId, String messageId, String languageId, String authorId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.languageId = languageId;
        this.authorId = authorId;
    }
}
