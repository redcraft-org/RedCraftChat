package org.redcraft.redcraftbungeechat.models.redcraft_website;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RedCraftUrlShortenerResponse {
    public boolean response;
    public String err;
    public String shortened;

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}