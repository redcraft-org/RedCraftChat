package org.redcraft.redcraftchat.models.translate;

import java.util.HashMap;

import org.redcraft.redcraftchat.models.SerializableModel;

public class TokenizedMessage extends SerializableModel {
    public String tokenizedMessage;
    public HashMap<String, String> tokenizedElements;

    public TokenizedMessage(String tokenizedMessage, HashMap<String, String> tokenizedElements) {
        this.tokenizedMessage = tokenizedMessage;
        this.tokenizedElements = tokenizedElements;
    }
}
