package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.PlayerLanguage;
import org.redcraft.redcraftchat.models.database.PlayerPreferences;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DatabasePlayerSource {

    Database db = DatabaseManager.getDatabase();

    public DatabasePlayerSource() {
    }

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        UUID playerUniqueId = player.getUniqueId();

        return db.where("player_uuid=?", playerUniqueId.toString())
                .first(PlayerPreferences.class);
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
    }

    public List<PlayerLanguage> getPlayerLanguages(ProxiedPlayer player) {
        return DatabaseManager.getDatabase().where("player_uuid=?", player.getUniqueId().toString())
                .results(PlayerLanguage.class);
    }

}
