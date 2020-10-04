package org.redcraft.redcraftchat.models.discord;

import org.redcraft.redcraftchat.models.SerializableModel;

public class TranslatedChannel extends SerializableModel {
    public String guildId;
    public String channelId;
    public String languageId;

    public TranslatedChannel(String guildId, String channelId, String languageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.languageId = languageId;
    }
}
