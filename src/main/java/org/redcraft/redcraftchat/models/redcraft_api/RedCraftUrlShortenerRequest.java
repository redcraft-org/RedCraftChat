package org.redcraft.redcraftchat.models.redcraft_api;

import org.redcraft.redcraftchat.models.SerializableModel;

public class RedCraftUrlShortenerRequest extends SerializableModel {
    public String url;
    public String shortened;

    public RedCraftUrlShortenerRequest(String url) {
        this.url = url;
        this.shortened = null;
    }

    public RedCraftUrlShortenerRequest(String url, String shortened) {
        this.url = url;
        this.shortened = shortened;
    }
}
