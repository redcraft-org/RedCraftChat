package org.redcraft.redcraftchat.models.modernmt;

import org.redcraft.redcraftchat.models.SerializableModel;

public class ModernmtTranslation extends SerializableModel {
    public String translation;

    public ModernmtTranslation(String translation) {
        this.translation = translation;
    }
}
