package org.redcraft.redcraftbungeechat.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DeeplTranslation {
    public String detectedSourceLanguage;
    public String text;

    public DeeplTranslation(String detectedSourceLanguage, String text) {
        this.detectedSourceLanguage = detectedSourceLanguage;
        this.text = text;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}