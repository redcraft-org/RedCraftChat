package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.Arrays;
import java.util.List;

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

    List<ChatColor> stylingCodes = Arrays.asList(
        ChatColor.BOLD,
        ChatColor.ITALIC,
        ChatColor.STRIKETHROUGH,
        ChatColor.UNDERLINE
    );

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChatEvent(ChatEvent event) {
        // TODO check commands and chat
        if (event.isProxyCommand() || event.isCommand() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        String message = ChatColor.translateAlternateColorCodes('&', event.getMessage());

        if (!player.hasPermission("redcraftchat.formatting.colors")) {
            message = ChatColor.stripColor(message);
        }

        if (!player.hasPermission("redcraftchat.formatting.styling")) {
            for (ChatColor bannedCode : stylingCodes) {
                message = message.replace(bannedCode.toString(), "");
            }
        }

        if (!player.hasPermission("redcraftchat.formatting.magic")) {
            message = message.replace(ChatColor.MAGIC.toString(), "");
        }

        BaseComponent[] formattedMessage = new ComponentBuilder("[" + player.getServer().getInfo().getName() + ChatColor.RESET + "][" + player.getDisplayName() + ChatColor.RESET + "] " + message).create();

        ProxyServer.getInstance().broadcast(formattedMessage);

        event.setCancelled(true);
    }
}
