package org.redcraft.redcraftchat.listeners.minecraft;

import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.dialog.NativeDialogSelector;
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

    // Long enough that somebody reading the dialog is not interrupted, short
    // enough that somebody staring at nothing is not stranded
    private static final long DIALOG_FALLBACK_SECONDS = 30;

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

    /**
     * The rung the ladder was missing.
     *
     * A dialog that is sent and never answered leaves the player unconfirmed
     * with nothing on screen and no way to know it. Every other route either
     * draws something or falls through immediately; this one waits on a reply
     * that may never come, so it needs a deadline of its own.
     */
    private void scheduleDialogFallback(Player player) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (!player.isActive()) {
                return;
            }
            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                if (preferences.languageSelectorConfirmed) {
                    return;
                }
                // Still unanswered, so the dialog either never drew or was
                // dismissed. Either way the player should be able to act.
                LanguageSelectorPrompt.sendFirstJoinPrompt(player, preferences);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Language dialog fallback failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).delay(DIALOG_FALLBACK_SECONDS, TimeUnit.SECONDS).schedule();
    }

    private void promptIfUnconfirmed(Player player) {
        if (!player.isActive()) {
            return;
        }

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            SelectorRoute route = LanguageSelectorManager.decideFor(player, preferences, Trigger.FIRST_JOIN);

            switch (route) {
                case DIALOG_FIRST_JOIN:
                    if (NativeDialogSelector.showPrimary(player, preferences)) {
                        scheduleDialogFallback(player);
                    } else {
                        // The client can show dialogs but this one did not
                        // reach it, so fall the whole way down rather than
                        // leaving the player with nothing
                        LanguageSelectorPrompt.sendFirstJoinPrompt(player, preferences);
                    }
                    break;

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
