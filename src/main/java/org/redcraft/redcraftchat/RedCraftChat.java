package org.redcraft.redcraftchat;

import net.dv8tion.jda.api.JDA;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.caching.providers.RedisCache;
import org.redcraft.redcraftchat.commands.discord.LangDiscordCommand;
import org.redcraft.redcraftchat.commands.discord.LinkMinecraftAccountDiscordCommand;
import org.redcraft.redcraftchat.commands.discord.PlayersDiscordCommand;
import org.redcraft.redcraftchat.commands.minecraft.BroadcastMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.CommandSpyMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.LangMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.LinkDiscordAccountMinecraftCommand;
import org.redcraft.redcraftchat.commands.minecraft.MailMinecraftCommand;
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
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftConnectMailListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftPlayerPreferencesListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftRemoteServerMessageListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftTabCompleteListener;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;
import org.redcraft.redcraftchat.runnables.DiscordUsersSynchronizerTask;
import org.redcraft.redcraftchat.runnables.LuckPermsSynchronizerTask;
import org.redcraft.redcraftchat.runnables.MinecraftServerStatusWatcherTask;
import org.redcraft.redcraftchat.runnables.ScheduledAnnouncementsTask;

@Plugin(id = "redcraftchat", name = "RedCraftChat", version = "0.1.0-SNAPSHOT", url = "https://redcraft.org", description = "I did it!", authors = {
		"RedCraft", "lululombard" })
public class RedCraftChat {

	private static RedCraftChat instance;

	@Inject
	public RedCraftChat() {
		setInstance(this);

		// Setup
		try {
			Config.readConfig(this);
		} catch (IOException e) {
			this.getLogger().severe("Could not read config.yml");
			e.printStackTrace();
			this.onDisable();
			return;
		}
		// TODO: Check if database is needed
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

		// Schedulers
		TaskScheduler scheduler = getProxy().getScheduler();
		scheduler.schedule(this, new DiscordChannelSynchronizerTask(), 3, 60, TimeUnit.SECONDS);
		scheduler.schedule(this, new DiscordUsersSynchronizerTask(), 3, 60, TimeUnit.SECONDS);
		scheduler.schedule(this, new LuckPermsSynchronizerTask(), 10, 30, TimeUnit.SECONDS);
		scheduler.schedule(this, new MinecraftServerStatusWatcherTask(), 5, 5, TimeUnit.SECONDS);
		scheduler.schedule(this, new ScheduledAnnouncementsTask(), Config.scheduledAnnouncementsInterval,
				Config.scheduledAnnouncementsInterval, TimeUnit.SECONDS);

		// Game listeners
		PluginManager pluginManager = this.getProxy().getPluginManager();
		pluginManager.registerListener(this, new MinecraftChatListener());
		pluginManager.registerListener(this, new MinecraftConnectDisconnectMessageListener());
		pluginManager.registerListener(this, new MinecraftConnectMailListener());
		pluginManager.registerListener(this, new MinecraftRemoteServerMessageListener());
		pluginManager.registerListener(this, new MinecraftPlayerPreferencesListener());
		if (Config.enableTabCompletion) {
			pluginManager.registerListener(this, new MinecraftTabCompleteListener());
		}

		// Game commands
		pluginManager.registerCommand(this, new BroadcastMinecraftCommand());
		pluginManager.registerCommand(this, new CommandSpyMinecraftCommand());
		pluginManager.registerCommand(this, new LangMinecraftCommand());
		pluginManager.registerCommand(this, new LinkDiscordAccountMinecraftCommand());
		pluginManager.registerCommand(this, new MailMinecraftCommand());
		pluginManager.registerCommand(this, new MsgMinecraftCommand());
		pluginManager.registerCommand(this, new MeMinecraftCommand());
		pluginManager.registerCommand(this, new PlayerSettingsMinecraftCommand());
		pluginManager.registerCommand(this, new ReplyMinecraftCommand());
	}

	@Override
	public void onDisable() {
		MinecraftDiscordBridge.getInstance()
				.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Server is shutting down!");
		getProxy().getScheduler().cancel(this);
		getProxy().getPluginManager().unregisterListeners(this);
		getProxy().getPluginManager().unregisterCommands(this);
		DiscordClient.getClient().shutdownNow();
		RedisCache.close();
	}

	public static void setInstance(RedCraftChat instance) {
		RedCraftChat.instance = instance;
	}

	public static RedCraftChat getInstance() {
		return instance;
	}
}