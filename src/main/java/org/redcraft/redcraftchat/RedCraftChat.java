package org.redcraft.redcraftchat;

import net.dv8tion.jda.api.JDA;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageDeletedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageEditedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageReceivedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordPlayersCommandListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftChatListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftPlayerPreferencesListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftRemoteServerMessageListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftTabCompleteListener;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;

public class RedCraftChat extends Plugin {

	private static RedCraftChat instance;

	private DiscordMessageReceivedListener discordMessageReceivedListener = new DiscordMessageReceivedListener();
	private DiscordMessageEditedListener discordMessageEditedListener = new DiscordMessageEditedListener();
	private DiscordMessageDeletedListener discordMessageDeletedListener = new DiscordMessageDeletedListener();
	private DiscordPlayersCommandListener discordPlayersCommandListener = new DiscordPlayersCommandListener();

	private DiscordChannelSynchronizerTask discordChannelSynchronizerTask = new DiscordChannelSynchronizerTask();

	private MinecraftChatListener minecraftChatListener = new MinecraftChatListener();
	private MinecraftRemoteServerMessageListener minecraftServerMessageListener = new MinecraftRemoteServerMessageListener();
	private MinecraftPlayerPreferencesListener minecraftPlayerPreferencesListener = new MinecraftPlayerPreferencesListener();
	private MinecraftTabCompleteListener minecraftTabCompleteListener = new MinecraftTabCompleteListener();

	@Override
	public void onEnable() {
		instance = this;

		// Setup
		Config.readConfig(this);
		DatabaseManager.connect();

		// Discord events
		JDA discordClient = DiscordClient.getClient();
		discordClient.addEventListener(discordMessageReceivedListener);
		discordClient.addEventListener(discordMessageEditedListener);
		discordClient.addEventListener(discordMessageDeletedListener);

		discordClient.addEventListener(discordPlayersCommandListener);

		discordClient.upsertCommand("players", "List players on Minecraft servers").queue();

		getLogger().info("Discord events registered");

		// Schedulers
		TaskScheduler scheduler = getProxy().getScheduler();
		scheduler.schedule(this, discordChannelSynchronizerTask, 5, 60, TimeUnit.SECONDS);

		// Game listeners
		PluginManager pluginManager = this.getProxy().getPluginManager();
		pluginManager.registerListener(this, minecraftChatListener);
		pluginManager.registerListener(this, minecraftServerMessageListener);
		pluginManager.registerListener(this, minecraftPlayerPreferencesListener);
		pluginManager.registerListener(this, minecraftTabCompleteListener);
	}

	@Override
	public void onDisable() {
		getProxy().getScheduler().cancel(this);
		getProxy().getPluginManager().unregisterListeners(this);
	}

	static public RedCraftChat getInstance() {
		return instance;
	}
}