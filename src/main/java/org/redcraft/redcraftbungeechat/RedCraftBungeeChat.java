package org.redcraft.redcraftbungeechat;

import net.md_5.bungee.api.plugin.Plugin;

import org.redcraft.redcraftbungeechat.discord.DiscordClient;
import org.redcraft.redcraftbungeechat.listeners.discord.MessageDeletedListener;
import org.redcraft.redcraftbungeechat.listeners.discord.MessageEditedListener;
import org.redcraft.redcraftbungeechat.listeners.discord.MessageReceivedListener;

public class RedCraftBungeeChat extends Plugin {

	private static Plugin instance;

	private MessageReceivedListener messageReceivedListener = new MessageReceivedListener();
	private MessageEditedListener messageEditedListener = new MessageEditedListener();
	private MessageDeletedListener messageDeletedListener = new MessageDeletedListener();

	@Override
	public void onEnable() {
		instance = this;

		Config.readConfig(this);

		DiscordClient.getClient().addEventListener(messageReceivedListener);
		DiscordClient.getClient().addEventListener(messageEditedListener);
		DiscordClient.getClient().addEventListener(messageDeletedListener);

		getLogger().info("Discord events registered");

		// TODO Schedulers here

		// TODO Listeners here
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