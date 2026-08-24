package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.runnables.LuckPermsSynchronizerTask;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public class MinecraftDisplayNameListener {

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        LuckPermsSynchronizerTask.updateUsername(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        DisplayNameManager.removeDisplayName(event.getPlayer().getUniqueId());
    }
}
