package org.redcraft.redcraftchat.listeners.minecraft;

import java.io.IOException;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public class MinecraftPlayerPreferencesListener {

    public class AsyncPostLoginEventParser implements Runnable {
        PostLoginEvent event;

        public AsyncPostLoginEventParser(PostLoginEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            // This will create player preferences if it does not exist already
            try {
                PlayerPreferencesManager.getPlayerPreferences(event.getPlayer());
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(PostLoginEvent event) {
        AsyncPostLoginEventParser postLoginEventParser = new AsyncPostLoginEventParser(event);

        RedCraftChat pluginInstance = RedCraftChat.getInstance();
        pluginInstance.getProxy().getScheduler().buildTask(pluginInstance, postLoginEventParser).schedule();
    }
}
