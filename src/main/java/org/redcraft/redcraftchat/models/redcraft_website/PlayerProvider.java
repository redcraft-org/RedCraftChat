package org.redcraft.redcraftchat.models.redcraft_website;

import java.util.UUID;

import org.redcraft.redcraftchat.models.SerializableModel;

import com.google.gson.annotations.SerializedName;

public class PlayerProvider extends SerializableModel {

    @SerializedName(value = "provider_name", alternate = { "providerName" })
    public String providerName;

    public UUID uuid;

    @SerializedName(value = "last_username", alternate = { "lastUsername" })
    public String lastUsername;

    @SerializedName(value = "previous_username", alternate = { "previousUsername" })
    public String previousUsername;
}
