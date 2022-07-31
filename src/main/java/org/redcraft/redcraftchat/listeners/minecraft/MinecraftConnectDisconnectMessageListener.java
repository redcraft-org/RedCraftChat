package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MinecraftConnectDisconnectMessageListener implements Listener {

    private Map<UUID, String> previousServers = new HashMap<UUID, String>();

    public class AsyncPlayerJoinHandler implements Runnable {
        ServerConnectedEvent event;

        AsyncPlayerJoinHandler(ServerConnectedEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            String previousServer = previousServers.get(event.getPlayer().getUniqueId());
            String currentServer = this.event.getServer().getInfo().getName();
            previousServers.put(event.getPlayer().getUniqueId(), currentServer);
            String message;
            if (previousServer != null && !previousServer.equals(currentServer)) {
                message = ChatColor.YELLOW + "%player% left the %previous_server% server and joined the %current_server% server";
            } else {
                message = ChatColor.YELLOW + "%player% joined the %current_server% server";
            }

            Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("%player%", event.getPlayer().getDisplayName());
            replacements.put("%previous_server%", previousServer);
            replacements.put("%current_server%", currentServer);

            // TODO make nice embeds
            MinecraftDiscordBridge.getInstance().broadcastMessage(message, replacements);
        }
    }

    public class AsyncPlayerLeaveHandler implements Runnable {
        PlayerDisconnectEvent event;

        AsyncPlayerLeaveHandler(PlayerDisconnectEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            previousServers.remove(event.getPlayer().getUniqueId());

            String message = ChatColor.YELLOW + "%player% left the server";

            Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("%player%", event.getPlayer().getName());

            MinecraftDiscordBridge.getInstance().broadcastMessage(message, replacements);
        }
    }

	@EventHandler
	public void onPlayerJoin(final ServerConnectedEvent e) {
		RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), new AsyncPlayerJoinHandler(e));
	}

	@EventHandler
	public void onPlayerSwitch(ServerDisconnectEvent e) {
		previousServers.put(e.getPlayer().getUniqueId(), e.getTarget().getName());
	}

	@EventHandler
	public void onPlayerLeave(PlayerDisconnectEvent e) {
		RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), new AsyncPlayerLeaveHandler(e));
	}
}
