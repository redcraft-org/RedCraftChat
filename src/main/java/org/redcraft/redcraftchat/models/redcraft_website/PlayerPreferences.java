package org.redcraft.redcraftchat.models.redcraft_website;

import java.util.List;

import org.redcraft.redcraftchat.models.SerializableModel;

import com.google.gson.annotations.SerializedName;

public class PlayerPreferences extends SerializableModel {

    @SerializedName(value = "main_language", alternate = {"mainLanguage"})
    public String mainLanguage;

    public String email;

    public List<String> languages;

    public List<PlayerProvider> providers;

    public PlayerPreferences() {
    }
}
