package org.redcraft.redcraftchat.runnables;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.messaging.announcements.providers.AnnouncementsProvider;
import org.redcraft.redcraftchat.messaging.announcements.providers.DatabaseAnnouncementsProvider;
import org.redcraft.redcraftchat.messaging.announcements.providers.RedCraftApiAnnouncementsProvider;

import net.md_5.bungee.api.ChatColor;

public class ScheduledAnnouncementsTask implements Runnable {

    Deque<String> messages = new ArrayDeque<>();

    public AnnouncementsProvider getAnnouncementsProvider() {
        switch (Config.scheduledAnnouncementsProvider) {
            case "database":
                return new DatabaseAnnouncementsProvider();

            case "api":
                return new RedCraftApiAnnouncementsProvider();

            default:
                throw new IllegalStateException("Unknown announcements provider: " + Config.scheduledAnnouncementsProvider);
        }
    }

    public void run() {
        // Do not send messages if no one is online
        if (RedCraftChat.getInstance().getProxy().getOnlineCount() == 0) {
            return;
        }

        if (messages.isEmpty()) {
            messages.addAll(getAnnouncementsProvider().getAnnouncements());
        }

        if (messages.isEmpty()) {
            return;
        }

        String message = ChatColor.GREEN + "[Auto announcement] " + ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', messages.pop());

        MinecraftDiscordBridge.getInstance().broadcastMinecraft(message, new HashMap<String, String>());

    }

}
