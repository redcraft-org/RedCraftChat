package org.redcraft.redcraftchat.models.translate;

import java.util.Map;

import org.redcraft.redcraftchat.models.SerializableModel;

public class TokenizedMessage extends SerializableModel {

    private String originalTokenizedMessage;
    private Map<String, String> tokenizedElements;

    public TokenizedMessage(String tokenizedMessage, Map<String, String> tokenizedElements) {
        this.originalTokenizedMessage = tokenizedMessage;
        this.tokenizedElements = tokenizedElements;
    }

    public String getOriginalTokenizedMessage() {
        return originalTokenizedMessage;
    }

    public Map<String, String> getTokenizedElements() {
        return tokenizedElements;
    }

    public void setOriginalTokenizedMessage(String originalTokenizedMessage) {
        this.originalTokenizedMessage = originalTokenizedMessage;
    }

    public void setTokenizedElement(Map<String, String> tokenizedElements) {
        this.tokenizedElements = tokenizedElements;
    }
}
