package org.redcraft.redcraftchat.listeners.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.PrivateFieldExtractor;
import org.redcraft.redcraftchat.translate.TranslationManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Chat;

public class MinecraftRemoteServerMessageListener implements Listener {

    public class AsyncChatParser implements Runnable {
        ServerConnectedEvent event;
        Chat chatPacket;

        AsyncChatParser(ServerConnectedEvent event, Chat chatPacket) {
            this.event = event;
            this.chatPacket = chatPacket;
        }

        @Override
        public void run() {
            Server server = event.getServer();
            ProxiedPlayer player = event.getPlayer();

            try {
                String rawJson = chatPacket.getMessage();

                BaseComponent[] messages = ComponentSerializer.parse(rawJson);

                ChatMessageType messageType;

                switch (chatPacket.getPosition()) {
                    case 2:
                        messageType = ChatMessageType.ACTION_BAR;
                        break;

                    default:
                        messageType = ChatMessageType.CHAT;
                        break;
                }

                MinecraftRemoteServerMessageListener.handleChatPacket(event.getServer(), event.getPlayer(), messages,
                        messageType);
            } catch (Exception e) {
                String messageTemplate = "Encountered an exception while parsing incoming message from server %s to player %s: %s";
                String errorMessage = String.format(messageTemplate, server.getInfo().getName(), player.getName(),
                        e.getMessage());
                RedCraftChat.getInstance().getLogger().severe(errorMessage);
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent event) {
        Server serverConnection = event.getServer();
        Object channelWrapper = PrivateFieldExtractor.extractPrivateApiField(serverConnection, "ch");
        Channel channel = (Channel) PrivateFieldExtractor.extractPrivateApiField(channelWrapper, "ch");

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addBefore("inbound-boss", "packet_interceptor", getPacketInterceptor(event));
    }

    public static void handleChatPacket(Server server, ProxiedPlayer player, BaseComponent[] messages, ChatMessageType position) {
        for (BaseComponent message : messages) {
            String messageTemplate = "[%s -> %s] %s";
            String debugMessage = String.format(messageTemplate, server.getInfo().getName(), player.getName(), message.toLegacyText());
            RedCraftChat.getInstance().getLogger().info(debugMessage);

            String translatedMessage = message.toLegacyText();
            try {
                // TODO this is very much temporary
                translatedMessage = TranslationManager.translate(translatedMessage, "EN", "FR");
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().severe("Error while translating message");
                e.printStackTrace();
            }

            BaseComponent[] translatedMessageComponents = new ComponentBuilder(translatedMessage).create();

            // Send original message
            player.sendMessage(position, translatedMessageComponents);
        }
    }

    private ChannelDuplexHandler getPacketInterceptor(ServerConnectedEvent event) {
        return new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                PacketWrapper wrapper = (PacketWrapper) message;

                if (wrapper.packet instanceof Chat) {
                    RedCraftChat pluginInstance = RedCraftChat.getInstance();
                    AsyncChatParser chatParser = new AsyncChatParser(event, (Chat) wrapper.packet);

                    pluginInstance.getProxy().getScheduler().runAsync(pluginInstance, chatParser);

                    return; // Do not forward original packet
                }

                super.channelRead(context, message); // send to client, not a Chat packet
            }
        };
    }
}
