package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ApiPlayerSource extends DatabasePlayerSource {

    static HttpClient httpClient = HttpClient.newHttpClient();

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(
                URI.create(Config.playerSourceApiUrl + player.getUniqueId().toString()))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get player preferences: " + response.statusCode() + " - " + response.body());
        }

        ProxyServer.getInstance().getLogger().info("[GET] Response: " + response.statusCode() + " - " + response.body() + " for " + player.getName());

        return new Gson().fromJson(response.body(), PlayerPreferences.class);
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        String playerUniqueId = preferences.minecraftUuid.toString();

        var request = HttpRequest.newBuilder(
                URI.create(Config.playerSourceApiUrl + playerUniqueId))
                .header("accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(new Gson().toJson(preferences)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to update player preferences: " + response.statusCode() + " - " + response.body());
        }

        ProxyServer.getInstance().getLogger()
                .info("[UPDATE] Response: " + response.statusCode() + " - " + response.body() + " for " + playerUniqueId);
    }
}
