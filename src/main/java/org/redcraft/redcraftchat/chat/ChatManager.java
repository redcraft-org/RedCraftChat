package org.redcraft.redcraftchat.chat;

import org.redcraft.redcraftchat.models.discord.TranslatedChannel;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public class ChatManager {

    private ChatManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void handleMinecraftServerMessage() {
        // TODO handle messages
    }

    public static void handleDiscordMinecraftMessage(Member member, TranslatedChannel channel, Message message) {
        // TODO handle messages
    }

    public static void handleMinecraftChatMessage() {
        // TODO handle messages
    }
}
