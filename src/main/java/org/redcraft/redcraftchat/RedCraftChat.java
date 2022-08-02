package org.redcraft.redcraftchat;

import net.dv8tion.jda.api.JDA;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.commands.discord.LangDiscordCommand;
import org.redcraft.redcraftchat.commands.discord.LinkMinecraftAccountDiscordCommand;
import org.redcraft.redcraftchat.commands.discord.PlayersDiscordCommand;
import org.redcraft.redcraftchat.commands.minecraft.BroadcastMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.CommandSpyMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.LangMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.LinkDiscordAccountMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.MeMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.MsgMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.PlayerSettingsMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.ReplyMinecraftCommand;
import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageDeletedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageEditedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageReceivedListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftChatListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftConnectDisconnectMessageListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftPlayerPreferencesListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftRemoteServerMessageListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftTabCompleteListener;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;
import org.redcraft.redcraftchat.runnables.DiscordUsersSynchronizerTask;
import org.redcraft.redcraftchat.runnables.LuckPermsSynchronizerTask;
import org.redcraft.redcraftchat.runnables.MinecraftServerStatusWatcherTask;

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
		discordClient.addEventListener(new PlayersDiscordCommand());
		discordClient.addEventListener(new LangDiscordCommand());
		discordClient.addEventListener(new LinkMinecraftAccountDiscordCommand());

		getLogger().info("Discord events registered");

		// Schedulers
		TaskScheduler scheduler = getProxy().getScheduler();
		scheduler.schedule(this, new DiscordChannelSynchronizerTask(), 3, 60, TimeUnit.SECONDS);
		scheduler.schedule(this, new DiscordUsersSynchronizerTask(), 3, 60, TimeUnit.SECONDS);
		scheduler.schedule(this, new LuckPermsSynchronizerTask(), 10, 30, TimeUnit.SECONDS);
		scheduler.schedule(this, new MinecraftServerStatusWatcherTask(), 5, 5, TimeUnit.SECONDS);

		// Game listeners
		PluginManager pluginManager = this.getProxy().getPluginManager();
		pluginManager.registerListener(this, new MinecraftChatListener());
		pluginManager.registerListener(this, new MinecraftConnectDisconnectMessageListener());
		pluginManager.registerListener(this, new MinecraftRemoteServerMessageListener());
		pluginManager.registerListener(this, new MinecraftPlayerPreferencesListener());
		pluginManager.registerListener(this, new MinecraftTabCompleteListener());

		// Game commands
		pluginManager.registerCommand(this, new BroadcastMinecraftCommand());
		pluginManager.registerCommand(this, new CommandSpyMinecraftCommand());
		pluginManager.registerCommand(this, new LangMinecraftCommand());
		pluginManager.registerCommand(this, new LinkDiscordAccountMinecraftCommand());
		pluginManager.registerCommand(this, new MsgMinecraftCommand());
		pluginManager.registerCommand(this, new MeMinecraftCommand());
		pluginManager.registerCommand(this, new PlayerSettingsMinecraftCommand());
		pluginManager.registerCommand(this, new ReplyMinecraftCommand());
	}

	@Override
	public void onDisable() {
		MinecraftDiscordBridge.getInstance().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Server is shutting down!");
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