package org.redcraft.redcraftchat.listeners.minecraft;

import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftServerDisconnectListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerDisconnectEvent(ServerDisconnectEvent event) {
        // TODO broadcast disconnect
    }
}
