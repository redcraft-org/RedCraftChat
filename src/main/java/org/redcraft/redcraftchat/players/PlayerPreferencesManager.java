package org.redcraft.redcraftchat.players;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.providers.DatabasePlayerProvider;
import org.redcraft.redcraftchat.players.providers.RedCraftApiPlayerProvider;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerPreferencesManager {

    static DatabasePlayerProvider playerProvider = null;

    public PlayerPreferencesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static DatabasePlayerProvider getPlayerProvider() {
        if (playerProvider == null) {
            switch (Config.playerProvider) {
                case "database":
                    playerProvider = new DatabasePlayerProvider();
                    break;

                case "api":
                    playerProvider = new RedCraftApiPlayerProvider();
                    break;

                default:
                    throw new IllegalStateException("Unknown database player source: " + Config.playerProvider);
            }
        }
        return playerProvider;
    }

    public static PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException {
        UUID playerUniqueId = player.getUniqueId();

        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager.get(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), PlayerPreferences.class);
        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = getPlayerProvider().getPlayerPreferences(player);

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
        if (playerPreferences.discordId != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerPreferences.discordId, playerPreferences);
        }

        return playerPreferences;
    }

    public static PlayerPreferences getPlayerPreferences(User user) throws IOException, InterruptedException {
        String discordId = user.getId();
        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager.get(CacheCategory.PLAYER_PREFERENCES, discordId, PlayerPreferences.class);

        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = getPlayerProvider().getPlayerPreferences(user);

        if (playerPreferences == null) {
            return null;
        }

        boolean updated = false;

        User discordUser = DiscordClient.getClient().getUserById(discordId);

        String discordName = discordUser.getName() + "#" + discordUser.getDiscriminator();

        if (!discordName.equals(playerPreferences.lastKnownDiscordName)) {
            // Detect username change
            playerPreferences.previousKnownDiscordName = playerPreferences.lastKnownDiscordName;
            playerPreferences.lastKnownDiscordName = discordName;
            updated = true;
        }

        if (updated) {
            updatePlayerPreferences(playerPreferences);
        }

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, String.valueOf(discordId), playerPreferences);
        if (playerPreferences.minecraftUuid != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerPreferences.minecraftUuid.toString(), playerPreferences);
        }
        if (playerPreferences.discordId != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerPreferences.discordId, playerPreferences);
        }

        return playerPreferences;
    }

    public static void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        getPlayerProvider().updatePlayerPreferences(preferences);

        if (preferences.minecraftUuid != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, preferences.minecraftUuid.toString(), preferences);
        }
        if (preferences.discordId != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, preferences.discordId, preferences);
        }
    }

    public static boolean playerSpeaksLanguage(PlayerPreferences preferences, String languageIsoCode) {
        if (preferences == null || preferences.languages == null) {
            return false;
        }

        for (String language : preferences.languages) {
            // This is a fix to use the ISO 639-1 code instead of the full locale code
            if (language.split("-")[0].equalsIgnoreCase(languageIsoCode)) {
                return true;
            }
        }

        return false;
    }

    public static String getMainPlayerLanguage(PlayerPreferences preferences) {
        if (preferences != null && preferences.mainLanguage != null) {
            // This is a fix to use the ISO 639-1 code instead of the full locale code
            return preferences.mainLanguage.split("-")[0];
        }

        return null;
    }

    public static String localizeMessageForPlayer(PlayerPreferences preferences, String message) {
        try {
            String messageLanguage = DetectionManager.getLanguage(message);

            if (messageLanguage == null) {
                messageLanguage = "en";
            }

            if (preferences == null || playerSpeaksLanguage(preferences, messageLanguage)) {
                return message;
            }

            return new TranslationManager(Config.upstreamTranslationProvider).translate(message, messageLanguage, preferences.mainLanguage);
        } catch (IOException | IllegalStateException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

    public static boolean toggleCommandSpy(ProxiedPlayer player) throws IOException, InterruptedException {
        PlayerPreferences preferences = getPlayerPreferences(player);

        preferences.commandSpyEnabled = !preferences.commandSpyEnabled;

        updatePlayerPreferences(preferences);

        return preferences.commandSpyEnabled;
    }

    public static String extractPlayerLanguage(ProxiedPlayer player) {
        try {
            String detectedLocale = player.getLocale().getLanguage() + "-" + player.getLocale().getCountry();
            if (LocaleManager.isSupportedLocale(detectedLocale)) {
                RedCraftChat.getInstance().getLogger().info("Detected language for " + player.getName() + ": " + detectedLocale);
                return detectedLocale;
            }
        } catch (NullPointerException e) {
            // TODO GeoIP test (User has a very old version of Minecraft or Minechat)
        }

        // Fallback
        return Config.translationDiscordSupportedLanguages.get(0);
    }

    public static boolean playerSpeaksLanguage(ProxiedPlayer player, String languageIsoCode) {
        try {
            return playerSpeaksLanguage(getPlayerPreferences(player), languageIsoCode);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public static String getMainPlayerLanguage(ProxiedPlayer player) {
        String playerLanguage = null;

        try {
            playerLanguage = getMainPlayerLanguage(getPlayerPreferences(player));
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (playerLanguage != null) {
            return playerLanguage;
        }

        return extractPlayerLanguage(player);
    }

    public static String localizeMessageForPlayer(ProxiedPlayer player, String message) {
        try {
            return localizeMessageForPlayer(getPlayerPreferences(player), message);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

    public static boolean playerSpeaksLanguage(User user, String languageIsoCode) {
        try {
            return playerSpeaksLanguage(getPlayerPreferences(user), languageIsoCode);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public static String getMainPlayerLanguage(User user) {
        try {
            return getMainPlayerLanguage(getPlayerPreferences(user));
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static String localizeMessageForPlayer(User user, String message) {
        try {
            return localizeMessageForPlayer(getPlayerPreferences(user), message);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

}
