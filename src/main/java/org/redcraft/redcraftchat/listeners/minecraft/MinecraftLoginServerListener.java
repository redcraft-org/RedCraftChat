package org.redcraft.redcraftchat.listeners.minecraft;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.servers.LoginServer;

/**
 * Puts a player where they asked to be put, rather than where the proxy would
 * have.
 *
 * The work is returned as an async EventTask rather than done inline. Reading
 * preferences goes to the database, and this event runs on the connection's
 * own thread during login: doing it inline would hold up every player's
 * handshake behind one query.
 */
public class MinecraftLoginServerListener {

    @Subscribe
    public EventTask onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        return EventTask.async(() -> {
            Player player = event.getPlayer();
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                String target = LoginServer.resolve(preferences.loginServer, preferences.lastServer);
                if (target == null) {
                    return;
                }

                RegisteredServer server = RedCraftChat.getInstance().getProxy()
                        .getServer(target).orElse(null);
                if (server == null) {
                    // The preference outlived the server it names. Saying so
                    // beats a silent fall back to the hub that looks like the
                    // setting was ignored.
                    RedCraftChat.getInstance().getLogger().info(
                            "{} wanted to land on {}, which the proxy does not have; using the default",
                            player.getUsername(), target);
                    return;
                }

                event.setInitialServer(server);
            } catch (Exception e) {
                // The proxy's own choice still applies, so the player lands
                // somewhere rather than nowhere
                RedCraftChat.getInstance().getLogger().warn(
                        "Could not pick a login server for {}: {}", player.getUsername(), e.getMessage());
            }
        });
    }
}
