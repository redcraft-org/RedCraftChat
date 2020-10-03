package org.redcraft.redcraftchat;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.listeners.discord.MessageDeletedListener;
import org.redcraft.redcraftchat.listeners.discord.MessageEditedListener;
import org.redcraft.redcraftchat.listeners.discord.MessageReceivedListener;
import org.redcraft.redcraftchat.runnables.DiscordChannelSynchronizerTask;

public class RedCraftChat extends Plugin {

	private static Plugin instance;

	private MessageReceivedListener messageReceivedListener = new MessageReceivedListener();
	private MessageEditedListener messageEditedListener = new MessageEditedListener();
	private MessageDeletedListener messageDeletedListener = new MessageDeletedListener();

	private DiscordChannelSynchronizerTask discordChannelSynchronizerTask = new DiscordChannelSynchronizerTask();

	@Override
	public void onEnable() {
		instance = this;

		// Setup
		Config.readConfig(this);
		DatabaseManager.connect();

		// Discord event
		DiscordClient.getClient().addEventListener(messageReceivedListener);
		DiscordClient.getClient().addEventListener(messageEditedListener);
		DiscordClient.getClient().addEventListener(messageDeletedListener);

		getLogger().info("Discord events registered");

		// TODO Schedulers here
		TaskScheduler scheduler = getProxy().getScheduler();
		scheduler.schedule(this, discordChannelSynchronizerTask, 0, 1, TimeUnit.MINUTES);

		// TODO Game listeners here
	}

	@Override
	public void onDisable() {
		getProxy().getScheduler().cancel(this);
		getProxy().getPluginManager().unregisterListeners(this);
	}

	static public Plugin getInstance() {
		return instance;
	}
}