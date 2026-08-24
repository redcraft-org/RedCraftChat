package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;

public class MinecraftTabCompleteListener {

    // Note: BungeeCord also had TabCompleteResponseEvent for backend server suggestions.
    // Velocity merges backend suggestions into the same TabCompleteEvent.

    @Subscribe(order = PostOrder.NORMAL)
    public void onTabCompleteEvent(TabCompleteEvent event) {
        this.addPlayerNameSuggestions(event.getSuggestions());
    }

    private void addPlayerNameSuggestions(List<String> suggestions) {
        boolean isPlayerNameSuggestion = false;

        // Check if it contains at least one player in the suggestions
        for (Player player : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            if (suggestions.contains(player.getUsername())) {
                isPlayerNameSuggestion = true;
                break;
            }
        }

        if (isPlayerNameSuggestion) {
            for (Player player : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
                String playerName = player.getUsername();
                if (!suggestions.contains(playerName)) {
                    suggestions.add(playerName);
                }
            }
        }
    }
}
