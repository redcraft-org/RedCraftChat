package org.redcraft.redcraftchat.runnables;

import org.redcraft.redcraftchat.discord.ChannelManager;

public class DiscordChannelSynchronizerTask implements Runnable {

    ChannelManager channelManager = new ChannelManager();

    public void run() {
        channelManager.syncLocaleRoles();
        channelManager.syncChannelCategories();
    }

}
