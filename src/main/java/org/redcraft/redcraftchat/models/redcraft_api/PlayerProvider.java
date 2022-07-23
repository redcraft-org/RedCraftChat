package org.redcraft.redcraftchat.models.redcraft_api;

import org.redcraft.redcraftchat.models.SerializableModel;

import com.google.gson.annotations.SerializedName;

public class PlayerProvider extends SerializableModel {

    // TODO swap provider_name and name once API is updated
    @SerializedName(value = "provider_name", alternate = { "name" })
    public String name;

    @SerializedName(value = "uuid", alternate = { "provider_uuid" })
    public String uuid;

    @SerializedName(value = "last_username", alternate = { "lastUsername" })
    public String lastUsername;

    @SerializedName(value = "previous_username", alternate = { "previousUsername" })
    public String previousUsername;

    public PlayerProvider() {
    }

    public PlayerProvider(String name, String uuid, String lastUsername, String previousUsername) {
        this.name = name;
        this.uuid = uuid;
        this.lastUsername = lastUsername;
        this.previousUsername = previousUsername;
    }
}
