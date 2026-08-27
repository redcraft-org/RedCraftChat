package org.redcraft.redcraftchat.displaykit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

/**
 * Registry and routing for the language selector.
 *
 * The whole "fallback not limitations" contract lives in {@link #decide}: a
 * pure function so the truth table is unit-tested, with one rider coded at
 * the call sites — any exception while building or presenting a surface
 * falls through to the chat variant, so a player is never left with nothing.
 */
public class LanguageSelectorManager {

    public enum Trigger {
        FIRST_JOIN,
        LANG_NO_ARGS,
        LANG_WITH_ARGS
    }

    public enum SelectorRoute {
        NONE,
        SURFACE_FIRST_JOIN,
        SURFACE_MANAGE,
        CHAT_MENU,
        CHAT_PROMPT
    }

    private static final Map<UUID, LanguageSelectorSession> sessions = new ConcurrentHashMap<>();

    private LanguageSelectorManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static SelectorRoute decide(boolean selectorEnabled, boolean libAvailable,
            boolean clientSupported, boolean confirmed, Trigger trigger) {
        boolean surfaceCapable = selectorEnabled && libAvailable && clientSupported;

        switch (trigger) {
            case FIRST_JOIN:
                if (confirmed) {
                    return SelectorRoute.NONE;
                }
                // The config flag gates the whole feature, prompt included.
                // Without this, turning the selector off still chat-prompts
                // every unconfirmed player on the network, which is the
                // opposite of a kill switch.
                if (!selectorEnabled) {
                    return SelectorRoute.NONE;
                }
                return surfaceCapable ? SelectorRoute.SURFACE_FIRST_JOIN : SelectorRoute.CHAT_PROMPT;
            case LANG_NO_ARGS:
                return surfaceCapable ? SelectorRoute.SURFACE_MANAGE : SelectorRoute.CHAT_MENU;
            case LANG_WITH_ARGS:
            default:
                // Arguments keep the exact legacy semantics on every client
                return SelectorRoute.CHAT_MENU;
        }
    }

    /** The decide() call with live state filled in. */
    public static SelectorRoute decideFor(Player player, PlayerPreferences preferences, Trigger trigger) {
        return decide(
                Config.displaykitSelectorEnabled,
                DisplayKitIntegration.isAvailable(),
                DisplayKitIntegration.isSupported(player),
                preferences.languageSelectorConfirmed,
                trigger);
    }

    /**
     * Opens a selector surface. Must run on a scheduler thread: label
     * resolution blocks on the translation cache.
     *
     * @return true when the surface was presented, false when the caller
     * should fall back to chat.
     */
    public static boolean openSurface(Player player, PlayerPreferences preferences, boolean firstJoin) {
        try {
            dismiss(player.getUniqueId());
            UUID playerId = player.getUniqueId();
            // Identity-keyed removal: a slow close from the previous session
            // must never evict the successor that replaced it
            LanguageSelectorSession[] self = new LanguageSelectorSession[1];
            LanguageSelectorSession session = new LanguageSelectorSession(player, preferences, firstJoin,
                    () -> sessions.remove(playerId, self[0]));
            self[0] = session;
            sessions.put(playerId, session);
            session.present();
            return true;
        } catch (Exception | LinkageError e) {
            sessions.remove(player.getUniqueId());
            RedCraftChat.getInstance().getLogger().warn(
                    "Could not present the language selector surface for {}, falling back to chat: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    public static void dismiss(UUID playerId) {
        LanguageSelectorSession session = sessions.remove(playerId);
        if (session != null) {
            session.closeQuietly();
        }
    }

    /**
     * The player respawned or changed server, so the surface's entities are
     * gone from their client whatever the session believes.
     */
    public static void onWorldChanged(UUID playerId) {
        LanguageSelectorSession session = sessions.remove(playerId);
        if (session != null) {
            session.onWorldChanged();
        }
    }

    /**
     * Preference-change hook, called from updatePlayerPreferences the same
     * way HologramTranslator's is. A live selector re-renders its labels in
     * the player's new language.
     */
    public static void onPreferencesUpdated(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LanguageSelectorSession session = sessions.get(playerId);
        if (session != null) {
            session.refresh();
        }
    }
}
