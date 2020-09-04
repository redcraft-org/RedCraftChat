package org.redcraft.redcraftbungeechat;

import net.md_5.bungee.api.plugin.Plugin;

public class RedCraftBungeeChat extends Plugin {

	private static Plugin instance;

	@Override
	public void onEnable() {
		instance = this;

		Config.readConfig(this);

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