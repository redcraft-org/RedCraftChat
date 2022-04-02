package org.redcraft.redcraftchat.helpers;

import org.redcraft.redcraftchat.RedCraftChat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class BasicMessageFormatter {

    private BasicMessageFormatter() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void sendInternalMessage(CommandSender target, String message, ChatColor color) {
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
