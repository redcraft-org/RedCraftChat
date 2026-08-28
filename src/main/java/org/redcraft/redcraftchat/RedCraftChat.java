package org.redcraft.redcraftchat;

import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.commands.CommandHints;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;

import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;

import net.dv8tion.jda.api.JDA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.locales.TranslationWarmer;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.caching.providers.RedisCache;
import org.redcraft.redcraftchat.commands.discord.LangDiscordCommand;
import org.redcraft.redcraftchat.commands.discord.LinkMinecraftAccountDiscordCommand;
import org.redcraft.redcraftchat.commands.discord.PlayersDiscordCommand;
import org.redcraft.redcraftchat.api.HttpApiServer;
import org.redcraft.redcraftchat.commands.minecraft.AliasMinecraftCommand;
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
import org.redcraft.redcraftchat.dialog.DialogClickListener;
import org.redcraft.redcraftchat.displaykit.DisplayKitIntegration;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftLanguageSelectorListener;
import org.redcraft.redcraftchat.listeners.packets.ChatSignatureStripper;
import org.redcraft.redcraftchat.listeners.packets.HologramTranslator;
import org.redcraft.redcraftchat.listeners.packets.SystemChatInterceptor;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;
import org.redcraft.redcraftchat.runnables.DiscordUsersSynchronizerTask;
import org.redcraft.redcraftchat.runnables.LuckPermsSynchronizerTask;
import org.redcraft.redcraftchat.runnables.MinecraftServerStatusWatcherTask;
import org.redcraft.redcraftchat.runnables.ScheduledAnnouncementsTask;

import org.slf4j.Logger;

@Plugin(id = "redcraftchat", name = RedCraftChat.PLUGIN_NAME, version = "0.1.7-SNAPSHOT", url = "https://redcraft.org", description = "Multi language chat and Discord bridge", authors = {
		"RedCraft" },
		// Soft: Floodgate drives the native Bedrock forms, and the
		// plugin has to load on a proxy that does not run it
		dependencies = { @Dependency(id = "floodgate", optional = true) })
public class RedCraftChat {

	public static final String PLUGIN_NAME = "RedCraftChat";

	private static RedCraftChat instance;

	private final HttpApiServer jsonApiServer = new HttpApiServer();

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
		if (Config.pretranslateUiEnabled) {
			// Runs once and off the startup path, the menus fall back to
			// translating on demand until it finishes
			scheduler.buildTask(this, new TranslationWarmer()).delay(15, TimeUnit.SECONDS).schedule();
		}
		scheduler.buildTask(this, new ScheduledAnnouncementsTask()).delay(Config.scheduledAnnouncementsInterval, TimeUnit.SECONDS).repeat(Config.scheduledAnnouncementsInterval, TimeUnit.SECONDS).schedule();

		// Packet listeners, they must be registered between load() and init()
		if (Config.stripChatSignatures) {
			PacketEvents.getAPI().getEventManager().registerListener(new ChatSignatureStripper());
		}
		PacketEvents.getAPI().getEventManager().registerListener(new SystemChatInterceptor());
		if (Config.translationEnabled && Config.translateHolograms) {
			PacketEvents.getAPI().getEventManager().registerListener(new HologramTranslator());
		}
		// The native dialog's return channel: dialog buttons make the client
		// send a custom click action, which crosses the proxy like any packet
		PacketEvents.getAPI().getEventManager().registerListener(new DialogClickListener());
		// DisplayKit registers its own packet listener, so it shares the same
		// pre-init window; a failure degrades to the chat selector
		DisplayKitIntegration.init(proxy);
		PacketEvents.getAPI().init();

		// Game listeners
		proxy.getEventManager().register(this, new MinecraftDisplayNameListener());
		proxy.getEventManager().register(this, new MinecraftChatListener());
		proxy.getEventManager().register(this, new MinecraftConnectDisconnectMessageListener());
		proxy.getEventManager().register(this, new MinecraftConnectMailListener());
		proxy.getEventManager().register(this, new MinecraftPlayerPreferencesListener());
		proxy.getEventManager().register(this, new MinecraftLanguageSelectorListener());
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
		jsonApiServer.start();


		// Aliases come from the config, so a server can be given a shortcut
		// without shipping a command for it
		for (Map.Entry<String, String> alias : Config.commandAliases.entrySet()) {
			commandManager.register(
				commandManager.metaBuilder(alias.getKey()).plugin(this).build(),
				new AliasMinecraftCommand(alias.getValue()));
		}

		// Hints are what a Bedrock client completes against: it never asks the
		// server for suggestions, it only reads the declared tree once. They
		// never execute, so they cannot change how anything parses.
		commandManager.register(commandManager.metaBuilder("broadcast").aliases("bc", "alert").plugin(this)
				.hint(CommandHints.text("message"))
				.build(), new BroadcastMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("commandspy").aliases("cspy").plugin(this)
				.hint(CommandHints.word("player"))
				.build(), new CommandSpyMinecraftCommand());
		commandManager.register(langMeta(commandManager), new LangMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("discord-link").plugin(this)
				.hint(CommandHints.leaf("unlink"))
				.hint(CommandHints.word("code"))
				.build(), new LinkDiscordAccountMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("mail").plugin(this)
				.hint(CommandHints.leaf("list"))
				.hint(CommandHints.leaf("listall"))
				.hint(CommandHints.leaf("read"))
				.hint(CommandHints.leaf("next"))
				.hint(CommandHints.leaf("prev"))
				.hint(CommandHints.verbWith("show", MAIL_SLOTS))
				.hint(CommandHints.verbWith("open", MAIL_SLOTS))
				.hint(CommandHints.verbWith("page", MAIL_SLOTS))
				.hint(CommandHints.verbWithWordThenText("reply", "number", "message"))
				.hint(CommandHints.verbWithWordThenText("send", "player", "message"))
				.build(), new MailMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("msg").aliases("tell", "m", "w").plugin(this)
				.hint(CommandHints.verbWithWordThenText("player", "player", "message").getChild("player"))
				.build(), new MsgMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("me").plugin(this)
				.hint(CommandHints.text("message"))
				.build(), new MeMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("player-settings").plugin(this)
				.hint(CommandHints.word("player"))
				.build(), new PlayerSettingsMinecraftCommand());
		commandManager.register(commandManager.metaBuilder("reply").aliases("r").plugin(this)
				.hint(CommandHints.text("message"))
				.build(), new ReplyMinecraftCommand());
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
		jsonApiServer.stop();
		RedisCache.close();
		if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isInitialized()) {
			DisplayKitIntegration.shutdown();
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
	/**
	 * The /lang hints, including one literal per supported locale.
	 *
	 * The locale list comes from the database or the API and can be null when
	 * the provider is down, so this degrades to the verbs alone rather than
	 * refusing to register the command.
	 */
	/**
	 * Slot numbers, as literals so a Bedrock client can offer them. A page
	 * holds five rows, so these are the only values that can ever be valid.
	 */
	private static final List<String> MAIL_SLOTS = List.of("1", "2", "3", "4", "5");

	private CommandMeta langMeta(CommandManager commandManager) {
		CommandMeta.Builder builder = commandManager.metaBuilder("lang").aliases("languages").plugin(this)
				.hint(CommandHints.leaf("confirm"))
				.hint(CommandHints.leaf("panel"))
				.hint(CommandHints.leaf("dialog"));

		List<String> codes = new ArrayList<>();
		try {
			List<SupportedLocale> locales = LocaleManager.getSupportedLocales();
			if (locales != null) {
				for (SupportedLocale locale : locales) {
					codes.add(locale.code);
				}
			}
		} catch (Exception e) {
			getLogger().warn("Could not read the supported locales for command hints: {}", e.getMessage());
		}

		for (String code : codes) {
			builder.hint(CommandHints.leaf(code));
		}
		// Deliberately not a child of each code: making it one would force a
		// Bedrock player to type main after every language
		builder.hint(CommandHints.verbWith("main", codes));

		return builder.build();
	}


}