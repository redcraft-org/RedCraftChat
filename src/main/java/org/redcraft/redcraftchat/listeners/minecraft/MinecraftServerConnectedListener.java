package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;

import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftServerConnectedListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        // TODO broadcast connect
        String debugMessage = String.format("%s triggered ServerConnectedEvent to %s", event.getPlayer().getName(), event.getServer().getInfo().getName());
        RedCraftChat.getInstance().getLogger().info(debugMessage);
    }
}
