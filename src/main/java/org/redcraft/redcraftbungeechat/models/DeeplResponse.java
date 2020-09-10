package org.redcraft.redcraftbungeechat.models;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DeeplResponse {
    public ArrayList<DeeplTranslation> translations;

    public DeeplResponse(ArrayList<DeeplTranslation> translations) {
        this.translations = translations;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}