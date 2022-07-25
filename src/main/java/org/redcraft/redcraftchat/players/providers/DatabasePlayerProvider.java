package org.redcraft.redcraftchat.players.providers;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import com.dieselpoint.norm.Database;
import com.google.gson.Gson;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.PlayerPreferencesDatabase;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DatabasePlayerProvider implements PlayerSourceInterface {

    Database db = DatabaseManager.getDatabase();

    public DatabasePlayerProvider() {
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

    public PlayerPreferences getPlayerPreferences(User user) throws IOException, InterruptedException {
        PlayerPreferencesDatabase result = db.where("discord_id=?", user.getId()).first(PlayerPreferencesDatabase.class);

        if (result == null) {
            return null;
        }

        return transform(result);
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        PlayerPreferencesDatabase transformedPreferences = transformToDatabase(preferences);

        Database db = DatabaseManager.getDatabase();

        String query, params;

        if (transformedPreferences.id > 0) {
            query = "id=?";
            params = String.valueOf(transformedPreferences.id);
        } else if (transformedPreferences.minecraftUuid != null) {
            query = "minecraft_uuid=?";
            params = transformedPreferences.minecraftUuid;
        } else if (transformedPreferences.discordId != null) {
            query = "discord_id=?";
            params = transformedPreferences.discordId;
        } else {
            throw new IllegalStateException("No unique identifier found for player preferences");
        }

        boolean playerAlreadyExists = !db.where(query, params).results(PlayerPreferencesDatabase.class).isEmpty();

        // upsert is not supported with MySQL
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

        playerPreferences.discordId = preferences.discordId;
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

        playerPreferences.discordId = preferences.discordId;
        playerPreferences.lastKnownDiscordName = preferences.lastKnownDiscordName;
        playerPreferences.previousKnownDiscordName = preferences.previousKnownDiscordName;

        playerPreferences.languages = new Gson().toJson(preferences.languages);

        playerPreferences.mainLanguage = preferences.mainLanguage;

        playerPreferences.commandSpyEnabled = preferences.commandSpyEnabled;

        return playerPreferences;
    }
}
