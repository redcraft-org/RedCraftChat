package org.redcraft.redcraftchat.players;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager;
import org.redcraft.redcraftchat.listeners.packets.HologramTranslator;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.locales.UiTranslations;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.providers.DatabasePlayerProvider;
import org.redcraft.redcraftchat.players.providers.PlayerProvider;
import org.redcraft.redcraftchat.players.providers.RedCraftApiPlayerProvider;
import org.redcraft.redcraftchat.runnables.DiscordUsersSynchronizerTask;
import org.redcraft.redcraftchat.translate.TranslationManager;

import com.velocitypowered.api.proxy.Player;

import net.dv8tion.jda.api.entities.User;

public class PlayerPreferencesManager {

    static PlayerProvider playerProvider = null;

    private PlayerPreferencesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static PlayerProvider getPlayerProvider() {
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

    public static PlayerPreferences getPlayerPreferences(Player player) throws IOException, InterruptedException {
        return getPlayerPreferences(player, true);
    }

    public static PlayerPreferences getPlayerPreferences(Player player, boolean createIfNotFound) throws IOException, InterruptedException {
        UUID playerUniqueId = player.getUniqueId();

        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager.get(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), PlayerPreferences.class);
        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = getPlayerProvider().getPlayerPreferences(player, createIfNotFound);

        boolean updated = false;

        if (!player.getUsername().equals(playerPreferences.lastKnownMinecraftName)) {
            // Detect username change
            playerPreferences.previousKnownMinecraftName = playerPreferences.lastKnownMinecraftName;
            playerPreferences.lastKnownMinecraftName = player.getUsername();
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

    public static PlayerPreferences getPlayerPreferences(UUID playerUniqueId) throws IOException, InterruptedException {
        Player onlinePlayer = RedCraftChat.getInstance().getProxy().getPlayer(playerUniqueId).orElse(null);
        if (onlinePlayer != null) {
            return getPlayerPreferences(onlinePlayer, false);
        }

        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager.get(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), PlayerPreferences.class);
        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = getPlayerProvider().getPlayerPreferences(playerUniqueId);

        CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerUniqueId.toString(), playerPreferences);
        if (playerPreferences.discordId != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, playerPreferences.discordId, playerPreferences);
        }

        return playerPreferences;
    }

    public static PlayerPreferences getPlayerPreferences(String username, boolean searchMinecraft, boolean searchDiscord) throws IOException, InterruptedException {
        return getPlayerProvider().getPlayerPreferences(username, searchMinecraft, searchDiscord);
    }

    public static PlayerPreferences getPlayerPreferences(User user) throws IOException, InterruptedException {
        return getPlayerPreferences(user, true);
    }

    public static PlayerPreferences getPlayerPreferences(User user, boolean createIfNotFound) throws IOException, InterruptedException {
        String discordId = user.getId();
        PlayerPreferences cachedPlayerPreferences = (PlayerPreferences) CacheManager.get(CacheCategory.PLAYER_PREFERENCES, discordId, PlayerPreferences.class);

        if (cachedPlayerPreferences != null) {
            return cachedPlayerPreferences;
        }

        PlayerPreferences playerPreferences = getPlayerProvider().getPlayerPreferences(user, createIfNotFound);

        boolean updated = false;

        String discordName = user.getName();

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

    public static void deletePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        String debugMessage = "Deleting player preferences for " + preferences.internalUuid + " (Minecraft " + preferences.minecraftUuid + " and Discord " + preferences.discordId + ")";
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        getPlayerProvider().deletePlayerPreferences(preferences);

        if (preferences.minecraftUuid != null) {
            CacheManager.delete(CacheCategory.PLAYER_PREFERENCES, preferences.minecraftUuid.toString());
        }
        if (preferences.discordId != null) {
            CacheManager.delete(CacheCategory.PLAYER_PREFERENCES, preferences.discordId);
        }
    }

    public static void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException {
        getPlayerProvider().updatePlayerPreferences(preferences);

        if (preferences.minecraftUuid != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, preferences.minecraftUuid.toString(), preferences);
        }
        if (preferences.discordId != null) {
            CacheManager.put(CacheCategory.PLAYER_PREFERENCES, preferences.discordId, preferences);
        }

        // After the cache write, so the refresh resolves the new languages.
        // Holograms only resend on their own when their text changes, so a
        // language change has to push corrected packets itself.
        HologramTranslator.onPreferencesUpdated(preferences.minecraftUuid);
        LanguageSelectorManager.onPreferencesUpdated(preferences.minecraftUuid);
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

    public static String localizeMessageForPlayer(PlayerPreferences preferences, String message, String translationProvider) {
        if (translationProvider == null) {
            translationProvider = Config.upstreamTranslationProvider;
        }
        try {
            String messageLanguage = DetectionManager.getLanguage(message);

            if (messageLanguage == null) {
                messageLanguage = Config.defaultLocale.split("-")[0];
            }

            if (preferences == null || playerSpeaksLanguage(preferences, messageLanguage)) {
                return message;
            }

            return new TranslationManager(translationProvider).translate(message, messageLanguage, preferences.mainLanguage);
        } catch (IOException | IllegalStateException | URISyntaxException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Localizes one of the interface's own strings into the player's MAIN
     * language.
     *
     * Deliberately not localizeMessageForPlayer, which returns a message
     * untouched when the player understands the language it is already in.
     * That is right for something another player typed and wrong for a menu:
     * a French player who also reads English would get an English interface
     * for as long as English stayed in their list, which on the screen that
     * picks a main language is exactly backwards.
     *
     * Hand-written translations win, since the interface is a fixed set of
     * strings and a machine translating one line with nothing around it does
     * not know it is labelling a button. Anything not covered falls through
     * to the translator.
     */
    public static String localizeUiForPlayer(PlayerPreferences preferences, String message) {
        if (preferences == null || preferences.mainLanguage == null) {
            return message;
        }

        String embedded = UiTranslations.lookup(message, preferences.mainLanguage);
        if (embedded != null) {
            return embedded;
        }

        String source = Config.defaultLocale.split("-")[0];
        if (preferences.mainLanguage.split("-")[0].equalsIgnoreCase(source)) {
            return message;
        }

        try {
            return new TranslationManager(Config.upstreamTranslationProvider)
                    .translate(message, source, preferences.mainLanguage);
        } catch (IOException | IllegalStateException | URISyntaxException | InterruptedException e) {
            RedCraftChat.getInstance().getLogger().warn("Could not localize an interface string: {}", e.getMessage());
            return message;
        }
    }

    /** The interface localizer, for callers holding a Player. */
    public static String localizeUiForPlayer(Player player, String message) {
        try {
            return localizeUiForPlayer(getPlayerPreferences(player), message);
        } catch (IOException | InterruptedException e) {
            RedCraftChat.getInstance().getLogger().warn("Could not read preferences to localize an interface string: {}",
                    e.getMessage());
            return message;
        }
    }

    public static String localizeMessageForPlayer(PlayerPreferences preferences, String message) {
        return localizeMessageForPlayer(preferences, message, null);
    }

    public static void togglePlayerLocale(PlayerPreferences preferences, String locale) {
        if (!LocaleManager.isSupportedLocale(locale)) {
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }

        if (preferences.mainLanguage.equals(locale)) {
            throw new IllegalArgumentException("You cannot delete your main language, please set another one first");
        }

        if (preferences.languages.contains(locale)) {
            preferences.languages.remove(locale);
        } else {
            preferences.languages.add(locale);
        }

        // An explicit language action in any selector counts as confirming
        preferences.languageSelectorConfirmed = true;

        try {
            updatePlayerPreferences(preferences);
            triggerDiscordSync();
        } catch (IOException | InterruptedException e) {
            RedCraftChat.getInstance().getLogger().error("Failed to update player preferences");
            e.printStackTrace();
            throw new IllegalStateException("Failed to update player preferences, please try again later");
        }
    }

    public static boolean setMainPlayerLocale(PlayerPreferences preferences, String locale) {
        if (!LocaleManager.isSupportedLocale(locale)) {
            return false;
        }

        if (!preferences.languages.contains(locale)) {
            preferences.languages.add(locale);
        }

        preferences.mainLanguage = locale;
        preferences.languageSelectorConfirmed = true;

        try {
            updatePlayerPreferences(preferences);
            triggerDiscordSync();
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * The "keep what I have" confirm: Done on the selector surface or
     * /lang confirm in chat. Idempotent, and never called by auto-detection.
     */
    public static void confirmLanguageSelection(PlayerPreferences preferences) throws IOException, InterruptedException {
        if (preferences.languageSelectorConfirmed) {
            return;
        }
        preferences.languageSelectorConfirmed = true;
        updatePlayerPreferences(preferences);
    }

    public static boolean toggleCommandSpy(Player player) throws IOException, InterruptedException {
        PlayerPreferences preferences = getPlayerPreferences(player);

        preferences.commandSpyEnabled = !preferences.commandSpyEnabled;

        updatePlayerPreferences(preferences);

        return preferences.commandSpyEnabled;
    }

    public static String extractPlayerLanguage(Player player) {
        Locale locale = getPlayerLocale(player, 5);
        if (locale != null) {
            String detectedLocale = locale.getLanguage() + "-" + locale.getCountry();
            String debugMessage = "Detected language for " + player.getUsername() + ": " + detectedLocale;
            RedCraftChat.getInstance().getLogger().info(debugMessage);
            if (LocaleManager.isSupportedLocale(detectedLocale)) {
                return detectedLocale;
            }
        } else {
            // TODO GeoIP test (User has a very old version of Minecraft or Minechat)
            RedCraftChat.getInstance().getLogger().error("Failed to detect language for " + player.getUsername() + ", falling back to default locale");
        }

        // Fallback
        return Config.defaultLocale;
    }

    private static Locale getPlayerLocale(Player player, int maxTries) {
        int tries = 0;
        while (tries < maxTries) {
            Locale locale = player.getEffectiveLocale();
            if (locale != null) {
                return locale;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tries++;
        }

        return null;
    }

    public static boolean playerSpeaksLanguage(Player player, String languageIsoCode) {
        try {
            return playerSpeaksLanguage(getPlayerPreferences(player), languageIsoCode);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public static String getMainPlayerLanguage(Player player) {
        try {
            return getMainPlayerLanguage(getPlayerPreferences(player));
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return extractPlayerLanguage(player);
    }

    public static String localizeMessageForPlayer(Player player, String message, String translationProvider) {
        try {
            return localizeMessageForPlayer(getPlayerPreferences(player), message, translationProvider);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

    public static String localizeMessageForPlayer(Player player, String message) {
        return localizeMessageForPlayer(player, message, null);
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

    public static String localizeMessageForPlayer(User user, String message, String translationProvider) {
        try {
            return localizeMessageForPlayer(getPlayerPreferences(user), message, translationProvider);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

    public static String localizeMessageForPlayer(User user, String message) {
        return localizeMessageForPlayer(user, message, null);
    }

    public static void triggerDiscordSync() {
        RedCraftChat instance = RedCraftChat.getInstance();
        instance.getProxy().getScheduler().buildTask(instance, new DiscordUsersSynchronizerTask()).delay(10, TimeUnit.MILLISECONDS).schedule();
    }

}
