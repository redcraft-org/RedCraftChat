package org.redcraft.redcraftchat.helpers;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class BasicMessageFormatter {

    private BasicMessageFormatter() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void sendInternalMessage(CommandSource target, String message, String extra, NamedTextColor color) {
        String translatedMessage = message;
        if (target instanceof Player) {
            translatedMessage = PlayerPreferencesManager.localizeMessageForPlayer((Player) target, message);
        }

        TextComponent.Builder messageBuilder = prepareInternalMessage()
                .append(deserialize(translatedMessage).colorIfAbsent(color));
        if (!translatedMessage.equals(message)) {
            messageBuilder.hoverEvent(HoverEvent.showText(deserialize(message).colorIfAbsent(color)));
        }
        if (extra != null) {
            messageBuilder.append(Component.text(" ")).append(deserialize(extra));
        }
        target.sendMessage(messageBuilder.build());
    }

    public static void sendInternalMessage(CommandSource target, String message, NamedTextColor color) {
        sendInternalMessage(target, message, null, color);
    }

    public static void sendInternalError(CommandSource target, String message) {
        sendInternalError(target, message, null);
    }

    public static void sendInternalError(CommandSource target, String message, String extra) {
        sendInternalMessage(target, message, extra, NamedTextColor.RED);
    }

    public static TextComponent.Builder prepareInternalMessage() {
        return Component.text()
                .append(Component.text("[" + RedCraftChat.PLUGIN_NAME + "] ", NamedTextColor.GOLD));
    }

    public static Component deserialize(String legacyMessage) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
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

    public static String getDisplayNameWithoutRank(String displayName) {
        var parts = displayName.split(">");
        return parts.length > 1 ? parts[1] : displayName;
    }
}
