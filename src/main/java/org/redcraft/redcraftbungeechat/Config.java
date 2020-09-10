package org.redcraft.redcraftbungeechat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Config {

	public static String discordToken = "";

	public static String deeplToken = "";
	public static String deeplEndpoint = "";
	public static String deeplFormality = "";

	public static void readConfig(Plugin plugin) {
		Configuration config = getConfig(plugin);

		discordToken = config.getString("discord-token");

		deeplToken = config.getString("deepl-token");
		deeplEndpoint = config.getString("deepl-endpoint");
		deeplFormality = config.getString("deepl-formality");
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
