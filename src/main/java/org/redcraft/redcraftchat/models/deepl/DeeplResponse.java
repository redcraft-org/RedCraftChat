package org.redcraft.redcraftchat.models.deepl;

import java.util.ArrayList;

import org.redcraft.redcraftchat.models.SerializableModel;

public class DeeplResponse extends SerializableModel {
    public ArrayList<DeeplTranslation> translations;

    public DeeplResponse(ArrayList<DeeplTranslation> translations) {
        this.translations = translations;
    }
}
