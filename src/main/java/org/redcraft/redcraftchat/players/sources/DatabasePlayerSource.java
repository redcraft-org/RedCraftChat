package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;
import java.util.UUID;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.PlayerPreferencesDatabase;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DatabasePlayerSource implements PlayerSourceInterface {

    Database db = DatabaseManager.getDatabase();

    public DatabasePlayerSource() {
    }

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        UUID playerUniqueId = player.getUniqueId();

        return this.transform(db.where("player_uuid=?", playerUniqueId.toString())
                .first(PlayerPreferencesDatabase.class));
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        String playerUniqueId = preferences.minecraftUuid.toString();

        // upsert is not supported with MySQL
        Database db = DatabaseManager.getDatabase();
        // TODO transform the other way around
        boolean playerAlreadyExists = !db.where("player_uuid=?", playerUniqueId).results(PlayerPreferences.class)
                .isEmpty();
        if (playerAlreadyExists) {
            db.update(preferences);
        } else {
            db.insert(preferences);
        }
    }

    public PlayerPreferences transform(PlayerPreferencesDatabase preferences) {
        PlayerPreferences playerPreferences = new PlayerPreferences();
        playerPreferences.minecraftUuid = UUID.fromString(preferences.playerUniqueId);
        playerPreferences.discordId = Long.parseLong(preferences.discordId);
        playerPreferences.commandSpyEnabled = preferences.commandSpyEnabled;
        // TODO missing stuff
        return playerPreferences;
    }
}
