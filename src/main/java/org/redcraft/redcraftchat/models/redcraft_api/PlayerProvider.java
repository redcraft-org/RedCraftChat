package org.redcraft.redcraftchat.models.redcraft_api;

import org.redcraft.redcraftchat.models.SerializableModel;

import com.google.gson.annotations.SerializedName;

public class PlayerProvider extends SerializableModel {

    @SerializedName(value = "provider_name", alternate = { "providerName" })
    public String providerName;

    @SerializedName(value = "provider_uuid", alternate = { "providerUuid" })
    public String providerUuid;

    @SerializedName(value = "last_username", alternate = { "lastUsername" })
    public String lastUsername;

    @SerializedName(value = "previous_username", alternate = { "previousUsername" })
    public String previousUsername;

    public PlayerProvider() {
    }

    public PlayerProvider(String providerName, String uuid, String lastUsername, String previousUsername) {
        this.providerName = providerName;
        this.providerUuid = uuid;
        this.lastUsername = lastUsername;
        this.previousUsername = previousUsername;
    }
}
