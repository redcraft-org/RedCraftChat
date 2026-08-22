package org.redcraft.redcraftchat.players;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.proxy.Player;

/**
 * Velocity has no player display name concept, unlike BungeeCord.
 * This keeps the LuckPerms prefixed name (populated by LuckPermsSynchronizerTask)
 * for everything that used player.getDisplayName() on BungeeCord.
 */
public class DisplayNameManager {

    private static final ConcurrentHashMap<UUID, String> displayNames = new ConcurrentHashMap<UUID, String>();

    private DisplayNameManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String getDisplayName(Player player) {
        String displayName = displayNames.get(player.getUniqueId());
        return displayName == null ? player.getUsername() : displayName;
    }

    public static String getDisplayName(UUID uniqueId, String fallback) {
        String displayName = displayNames.get(uniqueId);
        return displayName == null ? fallback : displayName;
    }

    public static void setDisplayName(UUID uniqueId, String displayName) {
        displayNames.put(uniqueId, displayName);
    }

    public static void removeDisplayName(UUID uniqueId) {
        displayNames.remove(uniqueId);
    }
}
