package org.redcraft.redcraftchat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;

import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;

import net.dv8tion.jda.api.JDA;

import java.io.IOException;
import java.nio.file.Path;
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
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageDeletedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageEditedListener;
import org.redcraft.redcraftchat.listeners.discord.DiscordMessageReceivedListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftChatListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftConnectDisconnectMessageListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftConnectMailListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftDisplayNameListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftPlayerPreferencesListener;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftTabCompleteListener;
import org.redcraft.redcraftchat.listeners.packets.ChatSignatureStripper;
import org.redcraft.redcraftchat.listeners.packets.SystemChatInterceptor;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;
import org.redcraft.redcraftchat.runnables.DiscordUsersSynchronizerTask;
import org.redcraft.redcraftchat.runnables.LuckPermsSynchronizerTask;
import org.redcraft.redcraftchat.runnables.MinecraftServerStatusWatcherTask;
import org.redcraft.redcraftchat.runnables.ScheduledAnnouncementsTask;

import org.slf4j.Logger;

@Plugin(id = "redcraftchat", name = RedCraftChat.PLUGIN_NAME, version = "0.1.0-SNAPSHOT", url = "https://redcraft.org", description = "Multi language chat and Discord bridge", authors = {
		"RedCraft" })
public class RedCraftChat {

	public static final String PLUGIN_NAME = "RedCraftChat";

	private static RedCraftChat instance;

	private final ProxyServer proxy;
	private final Logger logger;
	private final Path dataDirectory;

	@Inject
	public RedCraftChat(ProxyServer proxy, PluginContainer container, Logger logger, @DataDirectory Path dataDirectory) {
		this.proxy = proxy;
		this.logger = logger;
		this.dataDirectory = dataDirectory;

		setInstance(this);

		setupPacketEvents(container);
	}

	/**
	 * PacketEvents is shaded in, so we drive its lifecycle ourselves instead of
	 * depending on the standalone plugin. load() installs the internal listener
	 * that tracks client versions and connection states, so it has to run before
	 * any listener of ours is registered, and init() performs the netty pipeline
	 * injection, so it has to run after.
	 */
	private void setupPacketEvents(PluginContainer container) {
		PacketEventsSettings settings = new PacketEventsSettings()
				.reEncodeByDefault(true)
				.checkForUpdates(false)
				.kickOnPacketException(true)
				.kickIfTerminated(true)
				.debug(false);

		PacketEventsAPI<PluginContainer> api = VelocityPacketEventsBuilder.build(proxy, container, logger, dataDirectory, settings);
		PacketEvents.setAPI(api);
		api.load();
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		// Setup
		try {
			Config.readConfig(this);
		} catch (IOException e) {
			this.logger.error("Could not read config.yml", e);
			return;
		}
		// TODO: Check if database is needed
		DatabaseManager.connect();

		boolean discordReady = setupDiscord();

		// Schedulers
		Scheduler scheduler = proxy.getScheduler();
		if (discordReady) {
			scheduler.buildTask(this, new DiscordChannelSynchronizerTask()).delay(3, TimeUnit.SECONDS).repeat(60, TimeUnit.SECONDS).schedule();
			scheduler.buildTask(this, new DiscordUsersSynchronizerTask()).delay(3, TimeUnit.SECONDS).repeat(60, TimeUnit.SECONDS).schedule();
		}
		scheduler.buildTask(this, new LuckPermsSynchronizerTask()).delay(10, TimeUnit.SECONDS).repeat(30, TimeUnit.SECONDS).schedule();
		scheduler.buildTask(this, new MinecraftServerStatusWatcherTask()).delay(5, TimeUnit.SECONDS).repeat(5, TimeUnit.SECONDS).schedule();
		scheduler.buildTask(this, new ScheduledAnnouncementsTask()).delay(Config.scheduledAnnouncementsInterval, TimeUnit.SECONDS).repeat(Config.scheduledAnnouncementsInterval, TimeUnit.SECONDS).schedule();

		// Packet listeners, they must be registered between load() and init()
		if (Config.stripChatSignatures) {
			PacketEvents.getAPI().getEventManager().registerListener(new ChatSignatureStripper());
		}
		PacketEvents.getAPI().getEventManager().registerListener(new SystemChatInterceptor());
		PacketEvents.getAPI().init();

		// Game listeners
		proxy.getEventManager().register(this, new MinecraftDisplayNameListener());
		proxy.getEventManager().register(this, new MinecraftChatListener());
		proxy.getEventManager().register(this, new MinecraftConnectDisconnectMessageListener());
		proxy.getEventManager().register(this, new MinecraftConnectMailListener());
		proxy.getEventManager().register(this, new MinecraftPlayerPreferencesListener());
		if (Config.enableTabCompletion) {
			proxy.getEventManager().register(this, new MinecraftTabCompleteListener());
		}

		// Game commands
		// BungeeCord also claimed the namespaced minecraft:me and minecraft:tell
		// aliases so the backend copies could not be used to sidestep the proxy.
		// Velocity lowercases aliases without documenting what characters it accepts,
		// and a rejected alias aborts the rest of this method, so they are left out
		// until someone confirms them against a running proxy.
		CommandManager commandManager = proxy.getCommandManager();
		commandManager.register(commandManager.metaBuilder("broadcast").aliases("bc", "alert").plugin(this).build(), new BroadcastMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("commandspy").aliases("cspy").plugin(this).build(), new CommandSpyMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("lang").aliases("languages").plugin(this).build(), new LangMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("discord-link").plugin(this).build(), new LinkDiscordAccountMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("mail").plugin(this).build(), new MailMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("msg").aliases("tell", "m", "w").plugin(this).build(), new MsgMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("me").plugin(this).build(), new MeMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("player-settings").plugin(this).build(), new PlayerSettingsMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("reply").aliases("r").plugin(this).build(), new ReplyMinecraftCommand());
	}

	private boolean setupDiscord() {
		if (!Config.discordEnabled || Config.discordToken == null || Config.discordToken.isEmpty()) {
			this.logger.warn("Discord is disabled or no token is configured, skipping Discord setup");
			return false;
		}

		JDA discordClient;
		try {
			discordClient = DiscordClient.getClient();
		} catch (Exception e) {
			this.logger.error("Could not initialize the Discord client", e);
			return false;
		}

		if (discordClient == null) {
			return false;
		}

		// Discord events
		discordClient.addEventListener(new DiscordMessageReceivedListener());
		discordClient.addEventListener(new DiscordMessageEditedListener());
		discordClient.addEventListener(new DiscordMessageDeletedListener());

		// Discord commands
		discordClient.addEventListener(new PlayersDiscordCommand());
		discordClient.addEventListener(new LangDiscordCommand());
		discordClient.addEventListener(new LinkMinecraftAccountDiscordCommand());

		return true;
	}

	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		MinecraftDiscordBridge.getInstance().broadcastMessage(LegacyText.RED + LegacyText.BOLD + "Server is shutting down!");
		if (DiscordClient.hasClient()) {
			DiscordClient.getClient().shutdownNow();
		}
		RedisCache.close();
		if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isInitialized()) {
			PacketEvents.getAPI().terminate();
		}
	}

	public static void setInstance(RedCraftChat instance) {
		RedCraftChat.instance = instance;
	}

	public static RedCraftChat getInstance() {
		return instance;
	}

	public ProxyServer getProxy() {
		return proxy;
	}

	public Logger getLogger() {
		return logger;
	}

	public Path getDataDirectory() {
		return dataDirectory;
	}
}
