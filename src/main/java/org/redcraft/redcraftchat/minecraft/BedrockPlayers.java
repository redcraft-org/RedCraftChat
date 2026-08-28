package org.redcraft.redcraftchat.minecraft;

import java.util.UUID;

import com.velocitypowered.api.proxy.Player;

/**
 * Tells a Bedrock player apart from a Java one.
 *
 * Geyser presents Bedrock players to the proxy as a modern Java client, so
 * every protocol-version check answers yes for them. Anything that asks "can
 * this client render X" therefore has to ask this as well, or it will offer
 * Bedrock players a surface they cannot see.
 *
 * What they can actually use is narrow: typed commands, the section colour
 * codes, and the client's own command autocomplete. Chat components reach
 * them through Geyser's MessageTranslator, which serialises with the legacy
 * and plain-text serialisers, so click and hover events are dropped on the
 * way out and every clickable menu is decoration.
 */
public final class BedrockPlayers {

    private BedrockPlayers() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * Whether a UUID is one Floodgate minted for a Bedrock player.
     *
     * Floodgate builds these as new UUID(0, xuid), so the high half is zero,
     * and its own isFloodgateId is exactly this comparison. A Mojang account
     * is a version 4 UUID and can never have a zero high half.
     *
     * The username prefix (a leading dot by default) says the same thing, but
     * it is configurable and purely cosmetic, so it is not what we test on.
     */
    public static boolean isFloodgateUuid(UUID uuid) {
        return uuid != null && uuid.getMostSignificantBits() == 0L;
    }

    public static boolean isBedrock(Player player) {
        return player != null && isFloodgateUuid(player.getUniqueId());
    }
}
