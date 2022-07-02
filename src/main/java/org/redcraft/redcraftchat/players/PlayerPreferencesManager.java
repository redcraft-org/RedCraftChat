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

        PlayerPreferences playerPreferences = getPlayerSource().getPlayerPreferences(player); // TODO

        boolean updated = false;

        if (playerPreferences == null || playerPreferences.minecraftUuid == null) {
            playerPreferences = createPlayerPreferences(player);
            updated = true;
        } else if (!playerPreferences.lastKnownMinecraftName.equals(player.getName())) {
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

    public static PlayerPreferences createPlayerPreferences(ProxiedPlayer player) {
        PlayerPreferences playerPreferences = new PlayerPreferences(player.getUniqueId());

        String detectedPlayerLanguage = extractPlayerLanguage(player);

        String debugMessage = String.format("Detected language %s for player %s", detectedPlayerLanguage, player.getName());
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        playerPreferences.lastKnownMinecraftName = player.getName();

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
                if (language.equalsIgnoreCase(languageIsoCode)) {
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
                return preferences.mainLanguage;
            }
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        return Config.translationSupportedLanguages.get(0);
    }

}
