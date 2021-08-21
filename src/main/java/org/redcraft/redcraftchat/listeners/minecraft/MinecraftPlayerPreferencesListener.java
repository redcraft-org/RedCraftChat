package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.database.PlayerPreferencesManager;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MinecraftPlayerPreferencesListener implements Listener {

    public class AsyncPostLoginEventParser implements Runnable {
        PostLoginEvent event;

        public AsyncPostLoginEventParser(PostLoginEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            // This will create player preferences if it does not exist already
            PlayerPreferencesManager.getPlayerPreferences(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PostLoginEvent event) {
        AsyncPostLoginEventParser postLoginEventParser = new AsyncPostLoginEventParser(event);

        RedCraftChat pluginInstance = RedCraftChat.getInstance();
        pluginInstance.getProxy().getScheduler().runAsync(pluginInstance, postLoginEventParser);
    }
}
