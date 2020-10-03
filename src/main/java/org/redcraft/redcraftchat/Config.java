package org.redcraft.redcraftchat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Config {

	public static boolean discordEnabled = false;
	public static String discordToken = "";
	public static String discordChannelMinecraft = "minecraft";
	public static boolean discordActivityEnabled = true;
	public static String discordActivityType = "playing";
	public static String discordActivityValue = "RedCraft.org";

	public static boolean translationEnabled = false;
	public static String translationService = "deepl";
	public static List<String> translationSupportedLanguages = new ArrayList<String>();
	public static String translationDiscordCategoryFormat = "[%lang%] %topic%";
	public static String translationDiscordCategoryTemplate = "en";

	public static String deeplToken = "";
	public static String deeplEndpoint = "https://api.deepl.com/v2/translate";
	public static String deeplFormality = "normal";
	public static boolean deeplPreserveFormatting = false;

	public static boolean urlShorteningEnabled = false;
	public static String urlShorteningService = "redcraft_url_shortener";
	public static String urlShorteningEndpoint = "https://redcraft.org/api/v1/url";
	public static String urlShorteningToken = "";

	public static String databaseUri = "jdbc:sqlite:%plugin_config_path%/plugins/RedCraftChat/database.db";
	public static String databaseUsername = "";
	public static String databasePassword = "";

	public static boolean redisEnabled = false;
	public static String redisUri = "";
	public static String redisKeyPrefix = "rcbc";

	public static void readConfig(Plugin plugin) {
		Configuration config = getConfig(plugin);

		discordEnabled = config.getBoolean("discord-enabled");
		discordToken = config.getString("discord-token");
		discordChannelMinecraft = config.getString("discord-channel-minecraft");
		discordActivityEnabled = config.getBoolean("discord-activity-enabled");
		discordActivityType = config.getString("discord-activity-type");
		discordActivityValue = config.getString("discord-activity-value");

		translationEnabled = config.getBoolean("translation-enabled");
		translationService = config.getString("translation-service");
		translationSupportedLanguages = config.getStringList("translation-supported-languages");
		translationDiscordCategoryFormat = config.getString("translation-discord-category-format");
		translationDiscordCategoryTemplate = config.getString("translation-discord-category-template");

		deeplToken = config.getString("deepl-token");
		deeplEndpoint = config.getString("deepl-endpoint");
		deeplFormality = config.getString("deepl-formality");
		deeplPreserveFormatting = config.getBoolean("deepl-preserve-formatting");

		databaseUri = config.getString("database-uri");
		databaseUsername = config.getString("database-username");
		databasePassword = config.getString("database-password");

		redisEnabled = config.getBoolean("redis-enabled");
		redisUri = config.getString("redis-uri");
		redisKeyPrefix = config.getString("redis-key-prefix");
	}

	public static Configuration getConfig(Plugin plugin) {
		if (!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdir();
		}

		File configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
				try (InputStream is = plugin.getResourceAsStream("config.yml");
					OutputStream os = new FileOutputStream(configFile)) {
					ByteStreams.copy(is, os);
				}
			} catch (IOException e) {
				throw new RuntimeException("Unable to create configuration file", e);
			}
		}

		Configuration configuration;
		try {
			configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(plugin.getDataFolder(), "config.yml"));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return configuration;
	}
}
