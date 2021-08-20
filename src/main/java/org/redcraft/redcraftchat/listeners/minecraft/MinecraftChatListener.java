package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftChatListener implements Listener {

    private List<ChatColor> stylingCodes = Arrays.asList(
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

        event.setCancelled(true);

        MinecraftDiscordBridge.getInstance().translateAndPostMessage(player, message);
    }
}
