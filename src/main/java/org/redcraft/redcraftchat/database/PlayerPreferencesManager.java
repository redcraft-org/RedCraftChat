package org.redcraft.redcraftchat.database;

import java.util.UUID;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.database.PlayerPreferences;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerPreferencesManager {

    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) {
        return getPlayerPreferences(player.getUniqueId());
    }

    public PlayerPreferences getPlayerPreferences(UUID playerUniqueId) {
        Database db = DatabaseManager.getDatabase();

        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager
                .get(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), PlayerPreferences.class);
        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = db.where("id=?", playerUniqueId).first(PlayerPreferences.class);

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), playerPreferences);

        return playerPreferences;
    }

    public PlayerPreferences createPlayerPreferences(UUID playerUniqueId) {
        PlayerPreferences playerPreferences = new PlayerPreferences(playerUniqueId);

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerUniqueId);

        String detectedPlayerLanguage = extractPlayerLanguage(player);

        String debugMessage = String.format("Detected language %s for player %s", detectedPlayerLanguage, player.getName());
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        if (player != null) {
            playerPreferences.lastKnownName = player.getName();
        }

        // TODO call PlayerLanguageManager and add detected language

        return playerPreferences;
    }

    public void updatePlayerPreferences(PlayerPreferences preferences) {
        Database db = DatabaseManager.getDatabase();
        UUID playerUniqueId = preferences.uuid;

        boolean playerAlreadyExists = db.where("id=?", playerUniqueId).results(PlayerPreferences.class).size() > 0;
        if (playerAlreadyExists) {
            db.update(preferences);
        } else {
            db.insert(preferences);
        }

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), preferences);
    }

    private String extractPlayerLanguage(ProxiedPlayer player) {
        if (player != null) {
            return player.getLocale().getLanguage();
        }

        // TODO GeoIP test

        // Fallback
        return "EN";
    }
}
