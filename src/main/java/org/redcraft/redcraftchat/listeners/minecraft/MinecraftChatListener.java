package org.redcraft.redcraftchat.listeners.minecraft;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftChatListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChatEvent(ChatEvent event) {
        // TODO check commands and chat
        if (event.isProxyCommand() || event.isCommand() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        // TODO do actual formatting
        BaseComponent[] message = new ComponentBuilder("[" + player.getDisplayName() + ChatColor.RESET + "] " + event.getMessage()).create();

        ProxyServer.getInstance().broadcast(message);

        event.setCancelled(true);
    }
}
