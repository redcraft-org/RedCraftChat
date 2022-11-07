package org.redcraft.redcraftchat.models.deepl;

import org.redcraft.redcraftchat.models.SerializableModel;

public class DeeplTranslation extends SerializableModel {
    public String detectedSourceLanguage;
    public String text;

    public DeeplTranslation(String detectedSourceLanguage, String text) {
        this.detectedSourceLanguage = detectedSourceLanguage;
        this.text = text;
    }
}
