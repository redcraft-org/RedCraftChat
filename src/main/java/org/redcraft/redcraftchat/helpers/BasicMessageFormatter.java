package org.redcraft.redcraftchat.helpers;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BasicMessageFormatter {

    private BasicMessageFormatter() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void sendInternalMessage(CommandSender target, String message, ChatColor color) {
        if (target instanceof ProxiedPlayer) {
            message = PlayerPreferencesManager.localizeMessageForPlayer((ProxiedPlayer) target, message);
        }
        BaseComponent[] formattedMessage = prepareInternalMessage().append(message).color(color).create();
        target.sendMessage(formattedMessage);
    }

    public static void sendInternalError(CommandSender target, String message) {
        sendInternalMessage(target, message, ChatColor.RED);
    }

    public static ComponentBuilder prepareInternalMessage() {
        return new ComponentBuilder("[" + RedCraftChat.getInstance().getDescription().getName() + "] ").color(ChatColor.GOLD);
    }
}
