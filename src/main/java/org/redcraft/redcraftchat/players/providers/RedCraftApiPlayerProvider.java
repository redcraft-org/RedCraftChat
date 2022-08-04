package org.redcraft.redcraftchat.players.providers;

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
import org.redcraft.redcraftchat.models.redcraft_api.PlayerProviderApi;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class RedCraftApiPlayerProvider implements PlayerProvider {

    static HttpClient httpClient = HttpClient.newHttpClient();

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player, boolean createIfNotFound) throws IOException, InterruptedException {
        String url = Config.playerApiUrl + "/" + player.getUniqueId().toString() + "?isProvider=true";

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(url))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            if (createIfNotFound) {
                createPlayerPreferences(new PlayerPreferences(player));
                return getPlayerPreferences(player, false);
            } else {
                return null;
            }
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get player preferences: " + response.statusCode() + " - " + response.body());
        }

        return transform(new Gson().fromJson(response.body(), PlayerPreferenceApi.class));
    }

    public PlayerPreferences getPlayerPreferences(User user, boolean createIfNotFound) throws IOException, InterruptedException {
        String url = Config.playerApiUrl + "/" + user.getId() + "?isProvider=true";

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(url))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            if (createIfNotFound) {
                createPlayerPreferences(new PlayerPreferences(user));
                return getPlayerPreferences(user, false);
            } else {
                return null;
            }
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get player preferences: " + response.statusCode() + " - " + response.body());
        }

        return transform(new Gson().fromJson(response.body(), PlayerPreferenceApi.class));
    }

    public PlayerPreferences getPlayerPreferences(UUID player) throws IOException, InterruptedException {
        String url = Config.playerApiUrl + "/" + player.toString() + "?isProvider=true";

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(url))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            return null;
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get player preferences: " + response.statusCode() + " - " + response.body());
        }

        return transform(new Gson().fromJson(response.body(), PlayerPreferenceApi.class));
    }

    public PlayerPreferences getPlayerPreferences(String username, boolean searchMinecraft, boolean searchDiscord) throws IOException, InterruptedException {
        // TODO IMPLEMENT GET PLAYER PREFERENCES FROM USERNAME
        return null;
    }

    public void deletePlayerPreferences(PlayerPreferences playerPreferences) throws IOException, InterruptedException {
        String url = Config.playerApiUrl + "/" + playerPreferences.internalUuid;

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(url))
                .header("accept", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to delete player preferences: " + response.statusCode() + " - " + response.body());
        }
    }

    public void createPlayerPreferences(PlayerPreferences playerPreferences) throws IOException, InterruptedException {
        String body = new Gson().toJson(transformToApi(playerPreferences));

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(Config.playerApiUrl))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Failed to create player preferences: " + response.statusCode() + " - " + response.body());
        }
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        String url = Config.playerApiUrl + "/" + preferences.internalUuid;

        String body = new Gson().toJson(transformToApi(preferences));

        HttpRequest request = HttpRequest.newBuilder(
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

        for (PlayerProviderApi provider : preferences.providers) {
            switch (provider.name) {
                case "minecraft":
                    playerPreferences.minecraftUuid = UUID.fromString(provider.uuid);
                    playerPreferences.lastKnownMinecraftName = provider.lastUsername;
                    playerPreferences.previousKnownMinecraftName = provider.previousUsername;
                    break;

                case "discord":
                    playerPreferences.discordId = provider.uuid;
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

    public PlayerPreferenceApi transformToApi(PlayerPreferences preferences) {
        PlayerPreferenceApi playerPreferences = new PlayerPreferenceApi();

        playerPreferences.id = preferences.internalUuid;

        if (preferences.minecraftUuid != null) {
            playerPreferences.providers.add(new PlayerProviderApi("minecraft", preferences.minecraftUuid.toString(), preferences.lastKnownMinecraftName, preferences.previousKnownMinecraftName));
        }

        if (preferences.discordId != null) {
            playerPreferences.providers.add(new PlayerProviderApi("discord", preferences.discordId, preferences.lastKnownDiscordName, preferences.previousKnownDiscordName));
        }

        playerPreferences.languages = preferences.languages;

        playerPreferences.mainLanguage = preferences.mainLanguage;

        // TODO missing stuff
        return playerPreferences;
    }
}
