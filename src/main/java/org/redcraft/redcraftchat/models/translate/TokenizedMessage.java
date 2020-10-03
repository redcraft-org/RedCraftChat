package org.redcraft.redcraftchat.models.translate;

import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TokenizedMessage {
    public String tokenizedMessage;
    public HashMap<String, String> tokenizedElements;

    public TokenizedMessage(String tokenizedMessage, HashMap<String, String> tokenizedElements) {
        this.tokenizedMessage = tokenizedMessage;
        this.tokenizedElements = tokenizedElements;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}
