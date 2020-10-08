package org.redcraft.redcraftchat.models.discord;

import java.util.List;

import org.redcraft.redcraftchat.models.SerializableModel;

public class WebhookMessageMappingList extends SerializableModel {
    public List<WebhookMessageMapping> mappingList;
    public String originalLangId;

    public WebhookMessageMappingList(List<WebhookMessageMapping> mappingList, String originalLangId) {
        this.mappingList = mappingList;
        this.originalLangId = originalLangId;
    }
}
