package org.redcraft.redcraftbungeechat.runnables;

import org.redcraft.redcraftbungeechat.discord.ChannelManager;

public class DiscordChannelSynchronizerTask implements Runnable {

    ChannelManager channelManager = new ChannelManager();

    public void run() {
        channelManager.syncChannelCategories();
    }

}
