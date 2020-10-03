package org.redcraft.redcraftchat.models.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UserMessageMapping {
    public String guildId;
    public String channelId;
    public String messageId;

    public UserMessageMapping(String guildId, String channelId, String messageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}