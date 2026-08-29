package org.redcraft.redcraftchat.listeners.minecraft;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

/**
 * Remembers where a player was, so the selector can offer to send them back.
 *
 * Two moments write it, and they agree on what "last" means: the most recent
 * server that is not the one you are looking at now.
 *
 * Moving between servers records the one being left, which is what makes
 * "return to Survival" appear once you are back on the hub. Disconnecting
 * records the one you were standing on, which is the only thing that survives
 * to your next login: you always come back through the hub, so without this
 * the answer after a reconnect would be the hub itself, which is where you
 * already are.
 */
public class MinecraftLastServerListener {

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // Absent on the first connection of a session, which is the case that
        // must not overwrite what the previous session left behind
        event.getPreviousServer().ifPresent(
                previous -> remember(event.getPlayer(), previous.getServerInfo().getName()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(
                connection -> remember(player, connection.getServerInfo().getName()));
    }

    /**
     * Writes off the event thread, and only when the value actually changed:
     * a player hopping back and forth would otherwise buy a database write per
     * switch for a value that is already what it should be.
     */
    private void remember(Player player, String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return;
        }

        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                if (serverName.equals(preferences.lastServer)) {
                    return;
                }
                preferences.lastServer = serverName;
                PlayerPreferencesManager.updatePlayerPreferences(preferences);
            } catch (Exception e) {
                // Nothing a player can act on: the selector simply will not
                // offer a return until the next switch writes it
                plugin.getLogger().warn("Could not record the last server for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    /**
     * Which server to offer a return to, or null when there is nowhere worth
     * offering. Pure so the edge cases can be pinned without a proxy: nothing
     * recorded yet, and the recorded one being where they already are.
     */
    public static String returnTargetName(String lastServer, String currentServer) {
        if (lastServer == null || lastServer.isEmpty()) {
            return null;
        }
        if (lastServer.equals(currentServer)) {
            return null;
        }
        return lastServer;
    }

    /** The same answer, resolved against the servers the proxy actually has. */
    public static RegisteredServer returnTarget(Player player, PlayerPreferences preferences) {
        if (preferences == null) {
            return null;
        }

        String current = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse(null);
        String target = returnTargetName(preferences.lastServer, current);
        if (target == null) {
            return null;
        }

        // The name is whatever was recorded when they left, and a server can
        // be renamed or dropped from the proxy between then and now
        return RedCraftChat.getInstance().getProxy().getServer(target).orElse(null);
    }
}
