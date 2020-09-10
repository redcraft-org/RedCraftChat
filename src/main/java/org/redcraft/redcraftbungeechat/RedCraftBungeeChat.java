package org.redcraft.redcraftbungeechat;

import net.md_5.bungee.api.plugin.Plugin;

import org.redcraft.redcraftbungeechat.discord.DiscordClient;
import org.redcraft.redcraftbungeechat.listeners.discord.MessageReceivedListener;

public class RedCraftBungeeChat extends Plugin {

	private static Plugin instance;

	private MessageReceivedListener messageReceivedListener = new MessageReceivedListener();

	@Override
	public void onEnable() {
		instance = this;

		Config.readConfig(this);

		DiscordClient.getClient().addEventListener(messageReceivedListener);

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