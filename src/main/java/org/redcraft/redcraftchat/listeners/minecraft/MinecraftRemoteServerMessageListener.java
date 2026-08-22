package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.translate.TranslationManager;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

public class MinecraftRemoteServerMessageListener {

    static TranslationManager translationManager = new TranslationManager(Config.upstreamTranslationProvider);

    @Subscribe(order = PostOrder.FIRST)
    public void onServerConnected(ServerConnectedEvent event) {
        // TODO wave 2: the BungeeCord version injected a netty handler into the backend
        // connection through reflection to intercept SystemChat packets, translate them
        // per player and forward the translated component instead.
        // On Velocity this will be rebuilt on PacketEvents (already shaded in the jar):
        // listen for the clientbound system chat packet, run the translation off thread
        // and forward the translated component, keeping the original packet order.
    }
}
