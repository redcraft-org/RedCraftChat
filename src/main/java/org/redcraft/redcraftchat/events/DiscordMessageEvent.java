package org.redcraft.redcraftchat.events;

public class DiscordMessageEvent {
    public static DiscordMessageEvent instance = null;

    public static DiscordMessageEvent getInstance() {
        if (instance == null) {
            instance = new DiscordMessageEvent();
        }

        return instance;
    }

    public void triggerDiscordMessageEvent() {
        // TODO
    }
}
