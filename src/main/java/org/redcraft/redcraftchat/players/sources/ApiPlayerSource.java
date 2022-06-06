package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.dieselpoint.norm.Database;
import com.google.gson.Gson;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.database.PlayerPreferences;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ApiPlayerSource extends DatabasePlayerSource {

    static HttpClient httpClient = HttpClient.newHttpClient();

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(
                URI.create(Config.playerSourceApiUrl + player.getUniqueId().toString()))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return new Gson().fromJson(response.body(), PlayerPreferences.class);
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) {
        String playerUniqueId = preferences.playerUniqueId;

        // upsert is not supported with MySQL
        Database db = DatabaseManager.getDatabase();
        boolean playerAlreadyExists = !db.where("player_uuid=?", playerUniqueId).results(PlayerPreferences.class)
                .isEmpty();
        if (playerAlreadyExists) {
            db.update(preferences);
        } else {
            db.insert(preferences);
        }

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId, preferences);
    }

}
