package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.List;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftTabCompleteListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTabCompleteResponseEvent(TabCompleteResponseEvent event) {
        this.addPlayerNameSuggestions(event.getSuggestions());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTabCompleteEvent(TabCompleteEvent event) {
        this.addPlayerNameSuggestions(event.getSuggestions());
    }

    private void addPlayerNameSuggestions(List<String> suggestions) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            String playerName = player.getName();
            if (!suggestions.contains(playerName)) {
                suggestions.add(playerName);
            }
        }
    }
}
