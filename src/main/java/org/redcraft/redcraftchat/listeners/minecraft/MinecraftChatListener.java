package org.redcraft.redcraftchat.listeners.minecraft;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

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

    private List<ChatColor> stylingCodes = Arrays.asList(
        ChatColor.BOLD,
        ChatColor.ITALIC,
        ChatColor.STRIKETHROUGH,
        ChatColor.UNDERLINE
    );

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChatEvent(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer) || event.isCancelled()) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        if (event.isProxyCommand() || event.isCommand()) {
            handleCommandSpy(player, event.getMessage());
            return;
        }

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

    public void handleCommandSpy(ProxiedPlayer player, String message) {
        for (ProxiedPlayer potentialStaffMember : ProxyServer.getInstance().getPlayers()) {
            if (!player.equals(potentialStaffMember) && potentialStaffMember.hasPermission("redcraftchat.moderation.commandspy")) {
                PlayerPreferences playerPreferences;
                try {
                    playerPreferences = PlayerPreferencesManager.getPlayerPreferences(potentialStaffMember);
                    if (playerPreferences.commandSpyEnabled) {
                        BaseComponent[] formattedMessage = new ComponentBuilder("[CSPY][" + player.getDisplayName() + "] " + message).color(ChatColor.AQUA).create();
                        potentialStaffMember.sendMessage(formattedMessage);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
