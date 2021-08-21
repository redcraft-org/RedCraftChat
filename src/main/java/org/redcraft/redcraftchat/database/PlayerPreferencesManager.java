package org.redcraft.redcraftchat.database;

import java.util.UUID;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.database.PlayerLanguage;
import org.redcraft.redcraftchat.models.database.PlayerPreferences;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerPreferencesManager {

    public static PlayerPreferences getPlayerPreferences(ProxiedPlayer player) {
        UUID playerUniqueId = player.getUniqueId();
        Database db = DatabaseManager.getDatabase();

        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager
                .get(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), PlayerPreferences.class);
        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = db.where("player_uuid=?", playerUniqueId.toString()).first(PlayerPreferences.class);

        boolean updated = false;

        if (playerPreferences == null) {
            playerPreferences = createPlayerPreferences(player);
            updated = true;
        } else if (!playerPreferences.lastKnownName.equals(player.getName())) {
            // Detect username change
            playerPreferences.previousKnownName = playerPreferences.lastKnownName;
            playerPreferences.lastKnownName = player.getName();
            updated = true;
        }

        if (updated) {
            updatePlayerPreferences(playerPreferences);
        }

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), playerPreferences);

        return playerPreferences;
    }

    public static PlayerPreferences createPlayerPreferences(ProxiedPlayer player) {
        PlayerPreferences playerPreferences = new PlayerPreferences(player.getUniqueId());

        String detectedPlayerLanguage = extractPlayerLanguage(player);

        String debugMessage = String.format("Detected language %s for player %s", detectedPlayerLanguage, player.getName());
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        if (player != null) {
            playerPreferences.lastKnownName = player.getName();
        }

        return playerPreferences;
    }

    public static void updatePlayerPreferences(PlayerPreferences preferences) {
        String playerUniqueId = preferences.playerUniqueId;

        // upsert is not supported with MySQL
        Database db = DatabaseManager.getDatabase();
        boolean playerAlreadyExists = db.where("player_uuid=?", playerUniqueId).results(PlayerPreferences.class).size() > 0;
        if (playerAlreadyExists) {
            db.update(preferences);
        } else {
            db.insert(preferences);
        }

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId, preferences);
    }

    public static boolean toggleCommandSpy(ProxiedPlayer player) {
        PlayerPreferences preferences = getPlayerPreferences(player);

        preferences.commandSpyEnabled = !preferences.commandSpyEnabled;

        updatePlayerPreferences(preferences);

        return preferences.commandSpyEnabled;
    }

    public static boolean playerSpeaksLanguage(ProxiedPlayer player, String languageIsoCode) {
        PlayerPreferences playerPreferences = getPlayerPreferences(player);
        for (PlayerLanguage language : playerPreferences.languages()) {
            if (language.languageIso.equalsIgnoreCase(languageIsoCode)) {
                return true;
            }
        }

        return false;
    }

    public static String getMainPlayerLanguage(ProxiedPlayer player) {
        PlayerPreferences playerPreferences = getPlayerPreferences(player);
        for (PlayerLanguage language : playerPreferences.languages()) {
            if (language.isMainLanguage) {
                return language.languageIso;
            }
        }

        return extractPlayerLanguage(player);
    }

    public static String extractPlayerLanguage(ProxiedPlayer player) {
        try {
            return player.getLocale().getLanguage();
        } catch (NullPointerException e) {
            // TODO GeoIP test (User has a very old version of Minecraft or Minechat)
        }

        // Fallback
        return "EN";
    }
}
