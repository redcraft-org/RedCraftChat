package org.redcraft.redcraftchat.servers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.minecraft.ServerDisplayNameManager;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Moves a player between backends.
 *
 * Straight through the proxy API. /server is a proxy command and only works
 * on the proxy, which is where this already runs, so routing a transfer
 * through a command would mean handing it to a backend to hand back.
 */
public final class ServerTransfer {

    private ServerTransfer() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * Sends the player, or tells them why not.
     *
     * @return true when a connection was actually requested
     */
    public static boolean send(Player player, String serverName) {
        if (player == null || serverName == null || serverName.isEmpty()) {
            return false;
        }

        PlayerPreferences preferences = null;
        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(player);
        } catch (Exception e) {
            // Only costs the message its translation, so the transfer still
            // goes ahead below
            RedCraftChat.getInstance().getLogger().warn("Could not read preferences for {}: {}",
                    player.getUsername(), e.getMessage());
        }

        RegisteredServer server = RedCraftChat.getInstance().getProxy().getServer(serverName).orElse(null);
        if (server == null) {
            BasicMessageFormatter.sendInternalError(player, ui(preferences, UiStrings.SERVERS_GONE));
            return false;
        }

        boolean alreadyThere = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName().equals(serverName))
                .orElse(false);
        if (alreadyThere) {
            return false;
        }

        BasicMessageFormatter.sendInternalMessage(player,
                ui(preferences, UiStrings.SERVERS_SENDING)
                        .replace("%server%", ServerDisplayNameManager.getDisplayName(serverName)),
                NamedTextColor.GREEN);

        player.createConnectionRequest(server).fireAndForget();
        return true;
    }

    private static String ui(PlayerPreferences preferences, String message) {
        if (preferences == null) {
            return message;
        }
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }
}
