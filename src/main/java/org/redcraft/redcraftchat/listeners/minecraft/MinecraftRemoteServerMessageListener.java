package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.Arrays;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.helpers.PrivateFieldExtractor;
import org.redcraft.redcraftchat.translate.TranslationManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.SystemChat;

public class MinecraftRemoteServerMessageListener implements Listener {

    static TranslationManager translationManager = new TranslationManager(Config.upstreamTranslationProvider);

    public class AsyncChatParser implements Runnable {
        ServerConnectedEvent event;
        SystemChat chatPacket;
        long chatPacketTime;

        AsyncChatParser(ServerConnectedEvent event, SystemChat chatPacket) {
            this.event = event;
            this.chatPacket = chatPacket;
            this.chatPacketTime = System.nanoTime();
        }

        @Override
        public void run() {
            Server server = event.getServer();
            ProxiedPlayer player = event.getPlayer();

            try {
                BaseComponent message = chatPacket.getMessage();
                if (message instanceof TranslatableComponent) {
                    TranslatableComponent component = (TranslatableComponent) message;
                    String[] nonBroadcastableMessages = { "multiplayer.player.joined", "multiplayer.player.left" };
                    if (Arrays.asList(nonBroadcastableMessages).contains(component.getTranslate())) {
                        return;
                    }
                }

                ChatMessageType messageType;

                if (chatPacket.getPosition() == 2) {
                    messageType = ChatMessageType.ACTION_BAR;
                } else {
                    messageType = ChatMessageType.CHAT;
                }

                MinecraftRemoteServerMessageListener.handleChatPacket(chatPacketTime, event.getServer(),
                        event.getPlayer(), message, messageType);
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

        channel.pipeline().addBefore("inbound-boss", "redcraft-chat", getPacketInterceptor(event));
    }

    public static void handleChatPacket(long chatPacketTimestamp, Server server, ProxiedPlayer player,
            BaseComponent message, ChatMessageType position) throws InterruptedException {

        BaseComponent translatedMessageComponent = message;

        if (message instanceof TextComponent) {
            String translatedMessage = message.toLegacyText();

            try {
                String sourceLanguage = DetectionManager.getLanguage(translatedMessage);

                if (!PlayerPreferencesManager.playerSpeaksLanguage(player, sourceLanguage)) {
                    String targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(player);
                    if (sourceLanguage != null && !sourceLanguage.equalsIgnoreCase(targetLanguage)) {
                        translatedMessage = translationManager.translate(translatedMessage, sourceLanguage,
                                targetLanguage);
                        Text hover = new Text(message.toLegacyText());
                        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
                        translatedMessageComponent = new ComponentBuilder(translatedMessage).event(hoverEvent)
                                .create()[0];
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                String messageTemplate = "Error while translating message [%s -> %s] %s";
                String debugMessage = String.format(messageTemplate, server.getInfo().getName(), player.getName(),
                        message.toLegacyText());
                RedCraftChat.getInstance().getLogger().severe(debugMessage);
            }
        }

        // Send messages
        player.sendMessage(position, translatedMessageComponent);
    }

    private ChannelDuplexHandler getPacketInterceptor(ServerConnectedEvent event) {
        return new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                PacketWrapper wrapper = (PacketWrapper) message;

                if (wrapper.packet instanceof SystemChat) {
                    RedCraftChat pluginInstance = RedCraftChat.getInstance();
                    AsyncChatParser chatParser = new AsyncChatParser(event, (SystemChat) wrapper.packet);

                    pluginInstance.getProxy().getScheduler().runAsync(pluginInstance, chatParser);

                    return; // Do not forward original packet
                }

                super.channelRead(context, message); // send to client, not a Chat packet
            }
        };
    }
}
