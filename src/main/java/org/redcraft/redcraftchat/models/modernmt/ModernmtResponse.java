package org.redcraft.redcraftchat.models.modernmt;

import org.redcraft.redcraftchat.models.SerializableModel;

public class ModernmtResponse extends SerializableModel {
    public int status;
    public ModernmtTranslation data;

    public ModernmtResponse(ModernmtTranslation data) {
        this.data = data;
    }
}
