package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.atteo.evo.inflector.English;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MinecraftConnectMailListener {

    public class AsyncPostLoginEventHandler implements Runnable {
        PostLoginEvent event;

        AsyncPostLoginEventHandler(PostLoginEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            Player player = event.getPlayer();

            if (player == null || !player.isActive()) {
                return;
            }

            List<PlayerMail> unreadMessages = MailMessagesManager.getPlayerMail(player, true);

            if (!unreadMessages.isEmpty()) {
                String message = "You have " + unreadMessages.size() + " unread mail " + English.plural("message", unreadMessages.size()) + ", click on this message or type %command% to read them.";
                String localizedMessage = PlayerPreferencesManager.localizeMessageForPlayer(player, message).replace("%command%", LegacyText.GOLD + "/mail list" + LegacyText.LIGHT_PURPLE);
                Component formattedMessage = BasicMessageFormatter.prepareInternalMessage()
                    .append(BasicMessageFormatter.deserialize(localizedMessage).colorIfAbsent(NamedTextColor.LIGHT_PURPLE))
                    .hoverEvent(HoverEvent.showText(Component.text(message.replace("%command%", "/mail list"))))
                    .clickEvent(ClickEvent.runCommand("/mail list"))
                    .build();
                player.sendMessage(formattedMessage);
            }
        }
    }

	@Subscribe
	public void onPlayerJoin(final PostLoginEvent e) {
        // Delay by a second to make sure we logged the player switch
		RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), new AsyncPostLoginEventHandler(e)).delay(10, TimeUnit.SECONDS).schedule();
	}
}
