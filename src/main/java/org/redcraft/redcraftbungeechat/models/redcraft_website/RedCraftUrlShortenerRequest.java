package org.redcraft.redcraftbungeechat.models.redcraft_website;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RedCraftUrlShortenerRequest {
    public String token;
    public String url;
    public String shortened;

    public RedCraftUrlShortenerRequest(String token, String url) {
        this.token = token;
        this.url = url;
        this.shortened = null;
    }

    public RedCraftUrlShortenerRequest(String token, String url, String shortened) {
        this.token = token;
        this.url = url;
        this.shortened = shortened;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}