package org.redcraft.redcraftchat.helpers;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BasicMessageFormatter {

    private BasicMessageFormatter() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void sendInternalMessage(CommandSender target, String message, ChatColor color) {
        HoverEvent hoverEvent = new HoverEvent(Action.SHOW_TEXT, new Text(color + message));

        if (target instanceof ProxiedPlayer) {
            message = PlayerPreferencesManager.localizeMessageForPlayer((ProxiedPlayer) target, message);
        }

        BaseComponent[] formattedMessage = prepareInternalMessage().append(message).color(color).event(hoverEvent).create();
        target.sendMessage(formattedMessage);
    }

    public static void sendInternalError(CommandSender target, String message) {
        sendInternalMessage(target, message, ChatColor.RED);
    }

    public static ComponentBuilder prepareInternalMessage() {
        return new ComponentBuilder("[" + RedCraftChat.getInstance().getDescription().getName() + "] ").color(ChatColor.GOLD);
    }

    public static MessageEmbed generateDiscordMessage(User target, String title, String message, int color) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(PlayerPreferencesManager.localizeMessageForPlayer(target, title));
        builder.setDescription(PlayerPreferencesManager.localizeMessageForPlayer(target, message));
        builder.setColor(color);
        return builder.build();
    }

    public static MessageEmbed generateDiscordError(User target, String message) {
        return generateDiscordMessage(target, "Error", message, 0xFF0000);
    }
}
