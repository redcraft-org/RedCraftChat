package org.redcraft.redcraftchat.models.redcraft_api;

import org.redcraft.redcraftchat.models.SerializableModel;

public class RedCraftUrlShortenerRequest extends SerializableModel {
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
}
