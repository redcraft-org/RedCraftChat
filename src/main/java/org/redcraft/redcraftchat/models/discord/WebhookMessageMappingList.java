package org.redcraft.redcraftchat.models.discord;

import java.util.List;

import org.redcraft.redcraftchat.models.SerializableModel;

public class WebhookMessageMappingList extends SerializableModel {
    public List<WebhookMessageMapping> mappingList;

    public WebhookMessageMappingList(List<WebhookMessageMapping> mappingList) {
        this.mappingList = mappingList;
    }
}
