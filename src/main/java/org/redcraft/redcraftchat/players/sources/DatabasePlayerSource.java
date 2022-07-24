package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import com.dieselpoint.norm.Database;
import com.google.gson.Gson;

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

        PlayerPreferencesDatabase result = db.where("minecraft_uuid=?", playerUniqueId.toString()).first(PlayerPreferencesDatabase.class);

        if (result == null) {
            updatePlayerPreferences(new PlayerPreferences(player));
            return getPlayerPreferences(player);
        }

        return transform(result);
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        String playerUniqueId = preferences.minecraftUuid.toString();

        // upsert is not supported with MySQL
        Database db = DatabaseManager.getDatabase();
        boolean playerAlreadyExists = !db.where("minecraft_uuid=?", playerUniqueId).results(PlayerPreferencesDatabase.class).isEmpty();

        PlayerPreferencesDatabase transformedPreferences = transformToDatabase(preferences);

        if (playerAlreadyExists) {
            db.update(transformedPreferences);
        } else {
            db.insert(transformedPreferences);
        }
    }

    public PlayerPreferences transform(PlayerPreferencesDatabase preferences) {
        PlayerPreferences playerPreferences = new PlayerPreferences();

        playerPreferences.internalUuid = String.valueOf(preferences.id);

        playerPreferences.minecraftUuid = UUID.fromString(preferences.minecraftUuid);
        playerPreferences.lastKnownMinecraftName = preferences.lastKnownMinecraftName;
        playerPreferences.previousKnownDiscordName = preferences.previousKnownDiscordName;

        playerPreferences.discordId = Long.parseLong(preferences.discordId);
        playerPreferences.lastKnownDiscordName = preferences.lastKnownDiscordName;
        playerPreferences.previousKnownDiscordName = preferences.previousKnownDiscordName;

        playerPreferences.languages = Arrays.asList(new Gson().fromJson(preferences.languages, String[].class));

        playerPreferences.mainLanguage = preferences.mainLanguage;

        playerPreferences.commandSpyEnabled = preferences.commandSpyEnabled;

        return playerPreferences;
    }

    public PlayerPreferencesDatabase transformToDatabase(PlayerPreferences preferences) {
        PlayerPreferencesDatabase playerPreferences = new PlayerPreferencesDatabase();

        playerPreferences.id = Long.parseLong(preferences.internalUuid);

        playerPreferences.minecraftUuid = preferences.minecraftUuid.toString();
        playerPreferences.lastKnownMinecraftName = preferences.lastKnownMinecraftName;
        playerPreferences.previousKnownDiscordName = preferences.previousKnownDiscordName;

        playerPreferences.discordId = String.valueOf(preferences.discordId);
        playerPreferences.lastKnownDiscordName = preferences.lastKnownDiscordName;
        playerPreferences.previousKnownDiscordName = preferences.previousKnownDiscordName;

        playerPreferences.languages = new Gson().toJson(preferences.languages);

        playerPreferences.mainLanguage = preferences.mainLanguage;

        playerPreferences.commandSpyEnabled = preferences.commandSpyEnabled;

        return playerPreferences;
    }
}
