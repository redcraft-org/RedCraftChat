package org.redcraft.redcraftchat.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SerializableModel {

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}
