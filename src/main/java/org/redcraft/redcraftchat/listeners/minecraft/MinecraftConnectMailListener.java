package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.atteo.evo.inflector.English;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MinecraftConnectMailListener implements Listener {

    public class AsyncPostLoginEventHandler implements Runnable {
        PostLoginEvent event;

        AsyncPostLoginEventHandler(PostLoginEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            ProxiedPlayer player = event.getPlayer();

            if (player == null || !player.isConnected()) {
                return;
            }

            List<PlayerMail> unreadMessages = MailMessagesManager.getPlayerMail(player, true);

            if (!unreadMessages.isEmpty()) {
                String message = "You have " + unreadMessages.size() + " unread mail " + English.plural("message", unreadMessages.size()) + ", click on this message or type %command% to read them.";
                String localizedMessage = PlayerPreferencesManager.localizeMessageForPlayer(player, message).replace("%command%", ChatColor.GOLD + "/mail list" + ChatColor.LIGHT_PURPLE);
                ComponentBuilder messageBuilder = BasicMessageFormatter.prepareInternalMessage()
                    .append(localizedMessage)
                    .color(ChatColor.LIGHT_PURPLE)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(message.replace("%command%", "/mail list"))))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mail list"));
                player.sendMessage(messageBuilder.create());
            }
        }
    }

	@EventHandler
	public void onPlayerJoin(final PostLoginEvent e) {
        // Delay by a second to make sure we logged the player switch
		RedCraftChat.getInstance().getProxy().getScheduler().schedule(RedCraftChat.getInstance(), new AsyncPostLoginEventHandler(e), 10, TimeUnit.SECONDS);
	}
}
