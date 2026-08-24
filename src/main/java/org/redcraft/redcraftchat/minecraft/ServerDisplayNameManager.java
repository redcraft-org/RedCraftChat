package org.redcraft.redcraftchat.minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.LegacyText;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Resolves the readable name of a backend server from the motd it answers a
 * ping with.
 *
 * Messages used to name servers the way they are registered on the proxy, so
 * players were told they joined "crea_redstone_plot". Every backend already
 * advertises a proper name in its motd, so that is used instead and the
 * registered name is only a fallback.
 */
public class ServerDisplayNameManager {

    private static final Map<String, String> displayNames = new ConcurrentHashMap<String, String>();

    private ServerDisplayNameManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String getDisplayName(String serverName) {
        if (serverName == null) {
            return null;
        }

        // A name set in the config wins, it is the only one that can carry
        // colours and it does not depend on a backend answering a ping
        String configured = Config.serverDisplayNames.get(serverName);

        if (configured != null && !configured.isEmpty()) {
            return LegacyText.translateAlternateColorCodes('&', configured);
        }

        return displayNames.getOrDefault(serverName, serverName);
    }

    public static String getDisplayName(ServerInfo serverInfo) {
        return serverInfo == null ? null : getDisplayName(serverInfo.getName());
    }

    /**
     * Pings the server and keeps the first line of its motd. The ping is
     * asynchronous, so the registered name is served until the first answer
     * comes back.
     */
    public static void refresh(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();

        server.ping().thenAccept(ping -> {
            String motd = firstLine(ping.getDescriptionComponent());

            if (motd != null && !motd.isEmpty()) {
                displayNames.put(serverName, motd);
            }
        }).exceptionally(error -> {
            // An unreachable server keeps whatever name was resolved before,
            // the status watcher already reports that it is down
            return null;
        });
    }

    /**
     * Only pings a server whose name is not known yet. A motd rarely changes and
     * the watcher calling this runs every few seconds.
     */
    public static void refreshIfUnknown(RegisteredServer server) {
        if (!displayNames.containsKey(server.getServerInfo().getName())) {
            refresh(server);
        }
    }

    public static void refreshAll() {
        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
            refresh(server);
        }
    }

    private static String firstLine(Component description) {
        if (description == null) {
            return null;
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(description);

        return plain.split("\n")[0].trim();
    }
}
