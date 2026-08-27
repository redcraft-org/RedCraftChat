package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager.SelectorRoute;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager.Trigger;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorPrompt;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

/**
 * Prompts unconfirmed players to pick a language on the first server
 * connection of every session.
 *
 * ServerConnectedEvent rather than PostLoginEvent because the surface needs
 * the player standing in a world; the 3 second delay lets the spawn settle
 * and lands before the mail prompt at 10. A server switch dismisses any live
 * selector, since its display entities belong to the previous server.
 */
public class MinecraftLanguageSelectorListener {

    private static final long PROMPT_DELAY_SECONDS = 3;

    @Subscribe(order = PostOrder.NORMAL)
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        if (event.getPreviousServer().isPresent()) {
            // Switch: the surface's entities died with the old server
            LanguageSelectorManager.dismiss(player.getUniqueId());
            return;
        }

        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> promptIfUnconfirmed(player))
                .delay(PROMPT_DELAY_SECONDS, TimeUnit.SECONDS)
                .schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        LanguageSelectorManager.dismiss(event.getPlayer().getUniqueId());
    }

    private void promptIfUnconfirmed(Player player) {
        if (!player.isActive()) {
            return;
        }

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            SelectorRoute route = LanguageSelectorManager.decideFor(player, preferences, Trigger.FIRST_JOIN);

            switch (route) {
                case SURFACE_FIRST_JOIN:
                    if (LanguageSelectorManager.openSurface(player, preferences, true)) {
                        LanguageSelectorPrompt.sendSurfaceHint(player, preferences);
                    } else {
                        LanguageSelectorPrompt.sendFirstJoinPrompt(player, preferences);
                    }
                    break;
                case CHAT_PROMPT:
                    LanguageSelectorPrompt.sendFirstJoinPrompt(player, preferences);
                    break;
                case NONE:
                default:
                    break;
            }
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("First-join language prompt failed for {}: {}",
                    player.getUsername(), e.getMessage());
        }
    }
}
