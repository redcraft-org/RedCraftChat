package org.redcraft.redcraftchat.models.redcraft_api;

import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.models.SerializableModel;

import com.google.gson.annotations.SerializedName;

public class PlayerPreferenceApi extends SerializableModel {

    public String id;

    @SerializedName(value = "main_language", alternate = {"mainLanguage"})
    public String mainLanguage;

    public String email;

    public List<String> languages;

    public List<PlayerProvider> providers;

    public PlayerPreferenceApi() {
        languages = new ArrayList<String>();
        providers = new ArrayList<PlayerProvider>();
    }
}
