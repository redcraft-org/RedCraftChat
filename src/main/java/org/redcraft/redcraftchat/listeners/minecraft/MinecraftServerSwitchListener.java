package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;

import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftServerSwitchListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerSwitchEvent(ServerSwitchEvent event) {
        // TODO broadcast switch
        String debugMessage = String.format("%s triggered ServerSwitchEvent", event.getPlayer().getName());
        RedCraftChat.getInstance().getLogger().info(debugMessage);
    }
}
