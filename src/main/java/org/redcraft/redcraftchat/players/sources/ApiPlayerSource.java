package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.google.gson.Gson;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.models.redcraft_api.PlayerPreferenceApi;
import org.redcraft.redcraftchat.models.redcraft_api.PlayerProvider;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ApiPlayerSource extends DatabasePlayerSource {

    static HttpClient httpClient = HttpClient.newHttpClient();

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        String url = Config.playerSourceApiUrl + "/" + player.getUniqueId().toString() + "?isProvider=true";

        var request = HttpRequest.newBuilder(
                URI.create(url))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            createPlayerPreferences(player);
            return getPlayerPreferences(player);
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get player preferences: " + response.statusCode() + " - " + response.body());
        }

        return transform(new Gson().fromJson(response.body(), PlayerPreferenceApi.class));
    }

    public void createPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        String body = new Gson().toJson(transform(new PlayerPreferences(player)));

        var request = HttpRequest.newBuilder(
                URI.create(Config.playerSourceApiUrl))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new IOException("Failed to create player preferences: " + response.statusCode() + " - " + response.body());
        }
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        String url = Config.playerSourceApiUrl + "/" + preferences.internalUuid;

        String body = new Gson().toJson(transform(preferences));

        var request = HttpRequest.newBuilder(
                URI.create(url))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to update player preferences: " + response.statusCode() + " - " + response.body());
        }
    }

    public PlayerPreferences transform(PlayerPreferenceApi preferences) {
        PlayerPreferences playerPreferences = new PlayerPreferences();

        playerPreferences.internalUuid = preferences.id;

        for (PlayerProvider provider : preferences.providers) {
            switch (provider.name) {
                case "minecraft":
                    playerPreferences.minecraftUuid = UUID.fromString(provider.uuid);
                    playerPreferences.lastKnownMinecraftName = provider.lastUsername;
                    playerPreferences.previousKnownMinecraftName = provider.previousUsername;
                    break;

                case "discord":
                    playerPreferences.discordId = Long.parseLong(provider.uuid);
                    playerPreferences.lastKnownDiscordName = provider.lastUsername;
                    playerPreferences.previousKnownDiscordName = provider.previousUsername;
                    break;

                default:
                    break;
            }
        }

        playerPreferences.languages = preferences.languages;

        playerPreferences.mainLanguage = preferences.mainLanguage;

        // TODO missing stuff
        return playerPreferences;
    }

    public PlayerPreferenceApi transform(PlayerPreferences preferences) {
        PlayerPreferenceApi playerPreferences = new PlayerPreferenceApi();

        playerPreferences.id = preferences.internalUuid;

        if (preferences.minecraftUuid != null) {
            playerPreferences.providers.add(new PlayerProvider("minecraft", preferences.minecraftUuid.toString(), preferences.lastKnownMinecraftName, preferences.previousKnownMinecraftName));
        }

        if (preferences.discordId != 0) {
            playerPreferences.providers.add(new PlayerProvider("discord", Long.toString(preferences.discordId), preferences.lastKnownDiscordName, preferences.previousKnownDiscordName));
        }

        playerPreferences.languages = preferences.languages;

        playerPreferences.mainLanguage = preferences.mainLanguage;

        // TODO missing stuff
        return playerPreferences;
    }
}
