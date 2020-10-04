package org.redcraft.redcraftchat.models.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TranslatedChannel {
    public String guildId;
    public String channelId;
    public String languageId;

    public TranslatedChannel(String guildId, String channelId, String languageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.languageId = languageId;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}