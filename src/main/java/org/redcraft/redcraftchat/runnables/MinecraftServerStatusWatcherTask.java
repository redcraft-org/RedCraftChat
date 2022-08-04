package org.redcraft.redcraftchat.runnables;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;

public class MinecraftServerStatusWatcherTask implements Runnable {

    List<String> onlineServers = new ArrayList<String>();
    List<String> offlineServers = new ArrayList<String>();

    public boolean isServerOnline(ServerInfo server) {
        try (
            Socket socket = new Socket();
        ) {
            socket.connect(server.getSocketAddress(), 5);
            return true;
        } catch(IOException e) {
            return false;
        }
    }

    public void handleServerStatusChange(ServerInfo serverInfo, boolean online) {
        String message = ChatColor.GOLD + "[RedCraft] " + ChatColor.YELLOW  + "The Minecraft server %server% is now ";

        if (online) {
            message += ChatColor.GREEN + "" + ChatColor.BOLD + "available";
        } else {
            message += ChatColor.RED + "" + ChatColor.BOLD + "unavailable";
        }

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("%server%", serverInfo.getMotd() + ChatColor.YELLOW);

        MinecraftDiscordBridge.getInstance().broadcastMessage(message, replacements);
    }

    public void run() {
        for (Entry<String, ServerInfo> server : RedCraftChat.getInstance().getProxy().getServers().entrySet()) {
            if (isServerOnline(server.getValue())) {
                if (!onlineServers.contains(server.getKey())) {
                    onlineServers.add(server.getKey());
                    offlineServers.remove(server.getKey());
                    handleServerStatusChange(server.getValue(), true);
                }
            } else {
                if (!offlineServers.contains(server.getKey())) {
                    offlineServers.add(server.getKey());
                    onlineServers.remove(server.getKey());
                    handleServerStatusChange(server.getValue(), false);
                }
            }
        }
    }

}
