package org.redcraft.redcraftchat.players;

import java.io.IOException;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.sources.ApiPlayerSource;
import org.redcraft.redcraftchat.players.sources.DatabasePlayerSource;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerPreferencesManager {

    static DatabasePlayerSource playerSource = null;

    public PlayerPreferencesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static DatabasePlayerSource getPlayerSource() {
        if (playerSource == null) {
            switch (Config.playerSource) {
                case "database":
                    playerSource = new DatabasePlayerSource();
                    break;

                case "api":
                    playerSource = new ApiPlayerSource();
                    break;

                default:
                    throw new IllegalStateException("Unknown database player source: " + Config.playerSource);
            }
        }
        return playerSource;
    }

    public static PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        UUID playerUniqueId = player.getUniqueId();

        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager
                .get(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), PlayerPreferences.class);
        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = getPlayerSource().getPlayerPreferences(player);

        boolean updated = false;

        if (!player.getName().equals(playerPreferences.lastKnownMinecraftName)) {
            // Detect username change
            playerPreferences.previousKnownMinecraftName = playerPreferences.lastKnownMinecraftName;
            playerPreferences.lastKnownMinecraftName = player.getName();
            updated = true;
        }

        if (updated) {
            updatePlayerPreferences(playerPreferences);
        }

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), playerPreferences);

        return playerPreferences;
    }

    public static void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        getPlayerSource().updatePlayerPreferences(preferences);

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, preferences.minecraftUuid.toString(), preferences);
    }

    public static boolean toggleCommandSpy(ProxiedPlayer player) throws IOException, InterruptedException {
        PlayerPreferences preferences = getPlayerPreferences(player);

        preferences.commandSpyEnabled = !preferences.commandSpyEnabled;

        updatePlayerPreferences(preferences);

        return preferences.commandSpyEnabled;
    }

    public static boolean playerSpeaksLanguage(ProxiedPlayer player, String languageIsoCode) {
        try {
            PlayerPreferences preferences = getPlayerPreferences(player);

            for (String language : preferences.languages) {
                // This is a fix to use the ISO 639-1 code instead of the full locale code
                if (language.split("-")[0].equalsIgnoreCase(languageIsoCode)) {
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public static String getMainPlayerLanguage(ProxiedPlayer player) {
        try {
            PlayerPreferences preferences = getPlayerPreferences(player);

            if (preferences.mainLanguage != null) {
                // This is a fix to use the ISO 639-1 code instead of the full locale code
                return preferences.mainLanguage.split("-")[0];
            }
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return extractPlayerLanguage(player);
    }

    public static String extractPlayerLanguage(ProxiedPlayer player) {
        try {
            String detectedLanguage = player.getLocale().getLanguage() + "-" + player.getLocale().getCountry();
            RedCraftChat.getInstance().getLogger().info("Detected language for " + player.getName() + ": " + detectedLanguage);
            return detectedLanguage;
        } catch (NullPointerException e) {
            // TODO GeoIP test (User has a very old version of Minecraft or Minechat)
        }

        // Fallback
        return Config.translationSupportedLanguages.get(0);
    }

}
