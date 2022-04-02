package org.redcraft.redcraftchat;

import net.dv8tion.jda.api.JDA;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.commands.discord.PlayersCommand;
import org.redcraft.redcraftchat.commands.minecraft.CommandSpyCommand;
import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageDeletedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageEditedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageReceivedListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftChatListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftPlayerPreferencesListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftRemoteServerMessageListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftTabCompleteListener;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;

public class RedCraftChat extends Plugin {

	private static RedCraftChat instance;

	@Override
	public void onEnable() {
		setInstance(this);

		// Setup
		Config.readConfig(this);
		DatabaseManager.connect();

		// Discord events
		JDA discordClient = DiscordClient.getClient();
		discordClient.addEventListener(new DiscordMessageReceivedListener());
		discordClient.addEventListener(new DiscordMessageEditedListener());
		discordClient.addEventListener(new DiscordMessageDeletedListener());

		// Discord commands
		discordClient.addEventListener(new PlayersCommand());

		discordClient.upsertCommand("players", "List players on Minecraft servers").queue();

		getLogger().info("Discord events registered");

		// Schedulers
		TaskScheduler scheduler = getProxy().getScheduler();
		scheduler.schedule(this, new DiscordChannelSynchronizerTask(), 5, 60, TimeUnit.SECONDS);

		// Game listeners
		PluginManager pluginManager = this.getProxy().getPluginManager();
		pluginManager.registerListener(this, new MinecraftChatListener());
		pluginManager.registerListener(this, new MinecraftRemoteServerMessageListener());
		pluginManager.registerListener(this, new MinecraftPlayerPreferencesListener());
		pluginManager.registerListener(this, new MinecraftTabCompleteListener());

		// Commands
		pluginManager.registerCommand(this, new CommandSpyCommand());
	}

	@Override
	public void onDisable() {
		getProxy().getScheduler().cancel(this);
		getProxy().getPluginManager().unregisterListeners(this);
		getProxy().getPluginManager().unregisterCommands(this);
		DiscordClient.getClient().shutdownNow();
	}

	public static void setInstance(RedCraftChat instance) {
		RedCraftChat.instance = instance;
	}

	public static RedCraftChat getInstance() {
		return instance;
	}
}