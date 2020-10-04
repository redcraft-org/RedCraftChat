package org.redcraft.redcraftchat.models.deepl;

import org.redcraft.redcraftchat.models.SerializableModel;

public class DeeplSupportedLanguage extends SerializableModel {
    public String languageId;
    public boolean formalityAvailable;

    public DeeplSupportedLanguage(String languageId, boolean formalityAvailable) {
        this.languageId = languageId;
        this.formalityAvailable = formalityAvailable;
    }
}
