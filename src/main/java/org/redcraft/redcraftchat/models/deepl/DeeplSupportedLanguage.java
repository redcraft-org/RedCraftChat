package org.redcraft.redcraftchat.models.deepl;

public class DeeplSupportedLanguage {
    public String languageId;
    public boolean formalityAvailable;

    public DeeplSupportedLanguage(String languageId, boolean formalityAvailable) {
        this.languageId = languageId;
        this.formalityAvailable = formalityAvailable;
    }
}
