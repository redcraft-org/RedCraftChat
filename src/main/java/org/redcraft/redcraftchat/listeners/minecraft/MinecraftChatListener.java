package org.redcraft.redcraftchat.listeners.minecraft;

import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftChatListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChatEvent(ChatEvent event) {
        // TODO check commands and tchat
    }
}
