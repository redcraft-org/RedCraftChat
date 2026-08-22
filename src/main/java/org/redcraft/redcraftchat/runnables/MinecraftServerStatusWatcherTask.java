package org.redcraft.redcraftchat.runnables;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.LegacyText;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

public class MinecraftServerStatusWatcherTask implements Runnable {

    final int SCANS_COUNT = 3;

    List<String> onlineServers = new ArrayList<String>();
    Map<String, Integer> offlineServers = new HashMap<String, Integer>();

    public boolean isServerOnline(ServerInfo server) {
        try (
            Socket socket = new Socket();
        ) {
            socket.connect(server.getAddress(), 5);
            return true;
        } catch(IOException e) {
            return false;
        }
    }

    public void handleServerStatusChange(ServerInfo serverInfo, boolean online) {
        String message = LegacyText.GOLD + "[RedCraft] " + LegacyText.YELLOW  + "The Minecraft server %server% is now ";

        if (online) {
            message += LegacyText.GREEN + LegacyText.BOLD + "available";
        } else {
            message += LegacyText.RED + LegacyText.BOLD + "unavailable";
        }

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("%server%", serverInfo.getName() + LegacyText.YELLOW);

        MinecraftDiscordBridge.getInstance().broadcastMessage(message, replacements);
    }

    public void run() {
        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
            ServerInfo serverInfo = server.getServerInfo();
            String serverName = serverInfo.getName();
            if (isServerOnline(serverInfo)) {
                if (!onlineServers.contains(serverName)) {
                    RedCraftChat.getInstance().getLogger().info("Server " + serverName + " is marked as online");
                    onlineServers.add(serverName);
                    offlineServers.remove(serverName);
                    handleServerStatusChange(serverInfo, true);
                }
            } else {
                int failedScans = offlineServers.getOrDefault(serverName, 0) + 1;
                offlineServers.put(serverName, failedScans);
                if (failedScans < SCANS_COUNT + 1) {
                    RedCraftChat.getInstance().getLogger().warn("Server " + serverName + " seems to be offline. Failed scans: " + failedScans);
                } else if (onlineServers.contains(serverName)) {
                    RedCraftChat.getInstance().getLogger().warn("Server " + serverName + " is marked as offline");
                    onlineServers.remove(serverName);
                    handleServerStatusChange(serverInfo, false);
                }
            }
        }
    }

}
