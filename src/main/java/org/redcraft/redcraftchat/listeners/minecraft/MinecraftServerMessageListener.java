package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.DeObfuscation;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Chat;

public class MinecraftServerMessageListener implements Listener {

    @EventHandler (priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent event) {
        Server serverConnection = event.getServer();
        Object channelWrapper = DeObfuscation.extractPrivateApiField(serverConnection, "ch");
        Channel channel = (Channel) DeObfuscation.extractPrivateApiField(channelWrapper, "ch");

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addBefore("inbound-boss", "packet_interceptor", getPacketInterceptor(event));
    }

    public boolean handleChatPacket(Server server, ProxiedPlayer player, BaseComponent[] messages) {
        // The return value is:
        // - false if you don't want to cancel the original chat packet
        // - true if you want to cancel it and handle it yourself

        // Log messages
        for (BaseComponent message : messages) {
            String messageTemplate = "[Intercepted server message][%s -> %s] %s";
            String debugMessage = String.format(messageTemplate, server.getInfo().getName(), player.getName(), message.toLegacyText());
            RedCraftChat.getInstance().getLogger().info(debugMessage);
        }

        // Send nice JSON message
        player.sendMessage(messages);

        // Cancel original packet
        return true;
    }

    private ChannelDuplexHandler getPacketInterceptor(ServerConnectedEvent event) {
        MinecraftServerMessageListener listener = this;

        return new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                PacketWrapper wrapper = (PacketWrapper) msg;

                if (wrapper.packet instanceof Chat) {
                    Server server = event.getServer();
                    ProxiedPlayer player = event.getPlayer();

                    try {
                        Chat chat = (Chat) wrapper.packet;

                        String rawJson = chat.getMessage();

                        BaseComponent[] messages = ComponentSerializer.parse(rawJson).clone();

                        boolean shouldCancel = listener.handleChatPacket(event.getServer(), event.getPlayer(), messages);
                        if (shouldCancel) {
                            return; // don't send to client
                        }
                    } catch (Exception e) {
                        String messageTemplate = "Encountered an exception while parsing incoming message from server %s to player %s: %s\n%s";
                        String warningMessage = String.format(messageTemplate, server.getInfo().getName(), player.getName(), e.getMessage(), e.getStackTrace());
                        RedCraftChat.getInstance().getLogger().severe(warningMessage);
                    }
                }

                super.channelRead(ctx, msg); // send to client, not a Chat packet
            }
        };
    }
}
