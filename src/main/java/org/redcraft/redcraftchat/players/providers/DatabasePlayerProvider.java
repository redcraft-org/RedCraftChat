package org.redcraft.redcraftchat.players.providers;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import com.dieselpoint.norm.Database;
import com.google.gson.Gson;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.PlayerPreferencesDatabase;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DatabasePlayerProvider implements PlayerProvider {

    Database db = DatabaseManager.getDatabase();

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player, boolean createIfNotFound) throws IOException, InterruptedException {
        UUID playerUniqueId = player.getUniqueId();

        PlayerPreferencesDatabase result = db.where("minecraft_uuid=?", playerUniqueId.toString()).first(PlayerPreferencesDatabase.class);

        if (result == null) {
            if (createIfNotFound) {
                updatePlayerPreferences(new PlayerPreferences(player));
                return getPlayerPreferences(player, false);
            } else {
                return null;
            }
        }

        return transform(result);
    }

    public PlayerPreferences getPlayerPreferences(User user, boolean createIfNotFound) throws IOException, InterruptedException {
        PlayerPreferencesDatabase result = db.where("discord_id=?", user.getId()).first(PlayerPreferencesDatabase.class);

        if (result == null) {
            if (createIfNotFound) {
                updatePlayerPreferences(new PlayerPreferences(user));
                return getPlayerPreferences(user, false);
            } else {
                return null;
            }
        }

        return transform(result);
    }

    public PlayerPreferences getPlayerPreferences(UUID player) throws IOException, InterruptedException {
        PlayerPreferencesDatabase result = db.where("minecraft_uuid=?", player).first(PlayerPreferencesDatabase.class);

        if (result == null) {
            return null;
        }

        return transform(result);
    }

    public PlayerPreferences getPlayerPreferences(String username, boolean searchMinecraft, boolean searchDiscord) throws IOException, InterruptedException {
        PlayerPreferencesDatabase result = null;

        if (searchMinecraft) {
            result = db.where("last_known_minecraft_name=?", username).first(PlayerPreferencesDatabase.class);
        }

        if (result == null && searchDiscord) {
            result = db.where("last_known_discord_name=?", username).first(PlayerPreferencesDatabase.class);
        }

        if (result == null) {
            return null;
        }

        return transform(result);
    }

    public void deletePlayerPreferences(PlayerPreferences playerPreferences) throws IOException, InterruptedException {
        String debugMessage = "Deleting player preferences for Minecraft " + playerPreferences.minecraftUuid + " and Discord " + playerPreferences.discordId;
        RedCraftChat.getInstance().getLogger().info(debugMessage);
        db.delete(transformToDatabase(playerPreferences));
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        PlayerPreferencesDatabase transformedPreferences = transformToDatabase(preferences);

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
            try {
                db.insert(transformedPreferences);
            } catch (Exception e) {
                if (!e.getMessage().contains("Could not set value into pojo.")) {
                    throw e;
                }
            }
        }
    }

    public PlayerPreferences transform(PlayerPreferencesDatabase preferences) {
        PlayerPreferences playerPreferences = new PlayerPreferences();

        playerPreferences.internalUuid = String.valueOf(preferences.id);

        playerPreferences.minecraftUuid = preferences.minecraftUuid == null ? null : UUID.fromString(preferences.minecraftUuid);
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

        playerPreferences.id = preferences.internalUuid == null ? 0 : Long.parseLong(preferences.internalUuid);

        playerPreferences.minecraftUuid = preferences.minecraftUuid == null ? null : preferences.minecraftUuid.toString();
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
