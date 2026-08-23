package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.minecraft.ServerDisplayNameManager;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.runnables.LuckPermsSynchronizerTask;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

public class MinecraftConnectDisconnectMessageListener {

    private Map<UUID, String> previousServers = new HashMap<UUID, String>();

    public class AsyncPlayerJoinHandler implements Runnable {
        ServerConnectedEvent event;

        AsyncPlayerJoinHandler(ServerConnectedEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            LuckPermsSynchronizerTask.updateUsername(event.getPlayer());

            String previousServer = previousServers.get(event.getPlayer().getUniqueId());
            String currentServer = this.event.getServer().getServerInfo().getName();
            previousServers.put(event.getPlayer().getUniqueId(), currentServer);
            String message;
            if (previousServer != null && !previousServer.equals(currentServer)) {
                message = LegacyText.YELLOW + "%player% left the %previous_server% server and joined the %current_server% server";
            } else {
                message = LegacyText.YELLOW + "%player% joined the %current_server% server";
            }

            Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("%player%", DisplayNameManager.getDisplayName(event.getPlayer()) + LegacyText.YELLOW);
            replacements.put("%previous_server%", ServerDisplayNameManager.getDisplayName(previousServer) + LegacyText.YELLOW);
            replacements.put("%current_server%", ServerDisplayNameManager.getDisplayName(currentServer) + LegacyText.YELLOW);

            // TODO make nice embeds
            MinecraftDiscordBridge.getInstance().broadcastMessage(message, replacements);
        }
    }

    public class AsyncPlayerLeaveHandler implements Runnable {
        DisconnectEvent event;

        AsyncPlayerLeaveHandler(DisconnectEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            UUID playerUniqueId = event.getPlayer().getUniqueId();
            if (!previousServers.containsKey(playerUniqueId)) {
                return;
            }
            previousServers.remove(playerUniqueId);

            String message = LegacyText.YELLOW + "%player% " + LegacyText.YELLOW + "left the server";

            Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("%player%", DisplayNameManager.getDisplayName(event.getPlayer()));

            MinecraftDiscordBridge.getInstance().broadcastMessage(message, replacements);
        }
    }

	@Subscribe
	public void onPlayerJoin(final ServerConnectedEvent e) {
        // Delay by a second to make sure we logged the player switch
		RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), new AsyncPlayerJoinHandler(e)).delay(1, TimeUnit.SECONDS).schedule();
	}

	@Subscribe
	public void onPlayerLeave(DisconnectEvent e) {
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), new AsyncPlayerLeaveHandler(e)).schedule();
	}
}
