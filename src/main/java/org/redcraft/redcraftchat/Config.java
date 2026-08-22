package org.redcraft.redcraftchat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Config {

	public static boolean discordEnabled = false;
	public static String discordToken = "";
	public static String discordChannelMinecraft = "minecraft";
	public static boolean discordActivityEnabled = true;
	public static String discordActivityType = "playing";
	public static String discordActivityValue = "RedCraft.org";

	public static boolean translationEnabled = false;
	public static String chatTranslationProvider = "deepl";
	public static String upstreamTranslationProvider = "modernmt";
	public static List<String> translationDiscordSupportedLanguages = new ArrayList<String>();
	public static String translationDiscordCategoryFormat = "[%lang%] %topic%";

	public static String supportedLocalesProvider = "database";
	public static String supportedLocalesApiUrl = "https://redcraft.org/api/v1/language/list";
	public static String defaultLocale = "en-US";

	public static boolean enableTabCompletion = true;

	public static boolean stripChatSignatures = true;
	public static boolean stripLoginProfileKey = false;

	public static String deeplToken = "";
	public static String deeplEndpoint = "https://api.deepl.com/v2/translate";
	public static String deeplFormality = "normal";
	public static boolean deeplPreserveFormatting = false;

	public static String modernMtToken = "";

	public static boolean urlShorteningEnabled = false;
	public static String urlShorteningProvider = "redcraft";
	public static String urlShorteningEndpoint = "https://redcraft.org/api/v1/url";
	public static String urlShorteningToken = "";

	public static String playerAvatarApiEndpoint = "https://redcraft.org/api/v1/skin/head/%playername%?size=128";
	public static String playerAvatarFormat = "";

	public static String playerProvider = "database";
	public static String playerApiUrl = "https://redcraft.org/api/v1/player";

	public static String mailProvider = "database";

	public static String scheduledAnnouncementsProvider = "database";
	public static long scheduledAnnouncementsInterval = 900;

	public static String databaseUri = "jdbc:sqlite:%plugin_config_path%/plugins/RedCraftChat/database.db";
	public static String databaseUsername = "";
	public static String databasePassword = "";

	public static String cacheProvider = "memory";
	public static String redisUri = "";
	public static String redisKeyPrefix = "rcc";

	private Config() {
        throw new IllegalStateException("This class should not be instantiated");
    }

	public static void readConfig(RedCraftChat plugin) throws IOException {
		Map<String, Object> config = getConfig(plugin);

		if (config == null) {
			throw new IllegalStateException("Config is null!");
		}

		discordEnabled = getBoolean(config, "discord-enabled");
		discordToken = getString(config, "discord-token");
		discordChannelMinecraft = getString(config, "discord-channel-minecraft");
		discordActivityEnabled = getBoolean(config, "discord-activity-enabled");
		discordActivityType = getString(config, "discord-activity-type");
		discordActivityValue = getString(config, "discord-activity-value");

		translationEnabled = getBoolean(config, "translation-enabled");
		chatTranslationProvider = getString(config, "chat-translation-provider");
		upstreamTranslationProvider = getString(config, "upstream-translation-provider");
		translationDiscordSupportedLanguages = getStringList(config, "translation-discord-supported-languages");
		translationDiscordCategoryFormat = getString(config, "translation-discord-category-format");

		supportedLocalesProvider = getString(config, "supported-locales-provider");
		supportedLocalesApiUrl = getString(config, "supported-locales-api-url");
		defaultLocale = getString(config, "default-locale");

		enableTabCompletion = getBoolean(config, "enable-tab-completion");

		stripChatSignatures = getBoolean(config, "strip-chat-signatures", true);
		stripLoginProfileKey = getBoolean(config, "strip-login-profile-key", false);

		deeplToken = getString(config, "deepl-token");
		deeplEndpoint = getString(config, "deepl-endpoint");
		deeplFormality = getString(config, "deepl-formality");
		deeplPreserveFormatting = getBoolean(config, "deepl-preserve-formatting");

		modernMtToken = getString(config, "modernmt-token");

		urlShorteningEnabled = getBoolean(config, "url-shortening-enabled");
		urlShorteningProvider = getString(config, "url-shortening-provider");
		urlShorteningEndpoint = getString(config, "url-shortening-endpoint");
		urlShorteningToken = getString(config, "url-shortening-token");

		playerAvatarApiEndpoint = getString(config, "player-avatar-endpoint");
		playerAvatarFormat = getString(config, "player-avatar-format");

		playerProvider = getString(config, "player-provider");
		playerApiUrl = getString(config, "player-api-url");

		mailProvider = getString(config, "mail-provider");

		scheduledAnnouncementsProvider = getString(config, "scheduled-announcements-provider");
		scheduledAnnouncementsInterval = getLong(config, "scheduled-announcements-interval");

		databaseUri = getString(config, "database-uri");
		databaseUsername = getString(config, "database-username");
		databasePassword = getString(config, "database-password");

		cacheProvider = getString(config, "cache-provider");
		redisUri = getString(config, "redis-uri");
		redisKeyPrefix = getString(config, "redis-key-prefix");
	}

	public static Map<String, Object> getConfig(RedCraftChat plugin) throws IOException {
		Path dataDirectory = plugin.getDataDirectory();
		if (!Files.exists(dataDirectory)) {
			Files.createDirectories(dataDirectory);
		}

		Path configFile = dataDirectory.resolve("config.yml");
		if (!Files.exists(configFile)) {
			try (InputStream is = Config.class.getResourceAsStream("/config.yml")) {
				Files.copy(is, configFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		try (InputStream is = Files.newInputStream(configFile)) {
			return new Yaml().load(is);
		}
	}

	private static String getString(Map<String, Object> config, String key) {
		Object value = config.get(key);
		return value == null ? "" : String.valueOf(value);
	}

	private static boolean getBoolean(Map<String, Object> config, String key) {
		return getBoolean(config, key, false);
	}

	private static boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
		Object value = config.get(key);
		return value instanceof Boolean ? (Boolean) value : defaultValue;
	}

	private static long getLong(Map<String, Object> config, String key) {
		Object value = config.get(key);
		return value instanceof Number ? ((Number) value).longValue() : 0;
	}

	@SuppressWarnings("unchecked")
	private static List<String> getStringList(Map<String, Object> config, String key) {
		Object value = config.get(key);
		if (value instanceof List) {
			return (List<String>) value;
		}
		return new ArrayList<String>();
	}
}
