package org.redcraft.redcraftchat.bedrock;

import java.util.UUID;

import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

import org.redcraft.redcraftchat.RedCraftChat;

/**
 * The gate in front of Floodgate.
 *
 * Floodgate is a soft dependency: the proxy runs it today, but the plugin has
 * to load and behave on a proxy that does not. Every entry point here answers
 * false rather than throwing when the classes are absent, which is the same
 * shape DisplayKitIntegration uses for its own optional library.
 *
 * The linkage check is done once and cached. A NoClassDefFoundError is thrown
 * per call site otherwise, and catching it on every form send would hide a
 * real failure behind an expected one.
 */
public final class BedrockForms {

    private static Boolean available;

    private BedrockForms() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /** Whether Floodgate is present and its API is up. */
    public static synchronized boolean isAvailable() {
        if (available != null) {
            return available;
        }
        try {
            available = FloodgateApi.getInstance() != null;
        } catch (Exception | LinkageError e) {
            // Not installed, or installed and not started. Either way there
            // is nothing to send a form to.
            available = false;
        }
        if (!available) {
            RedCraftChat.getInstance().getLogger().info(
                    "Floodgate is not available, Bedrock players will use the typed commands");
        }
        return available;
    }

    /**
     * Sends a form to a Bedrock player.
     *
     * @return false when it could not be sent, so the caller can fall back
     * rather than assume the player is looking at something.
     */
    public static boolean send(UUID playerId, FormBuilder<?, ?, ?> form) {
        if (!isAvailable() || playerId == null) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().sendForm(playerId, form);
        } catch (Exception | LinkageError e) {
            RedCraftChat.getInstance().getLogger().warn("Could not send a Bedrock form: {}", e.getMessage());
            return false;
        }
    }

    /** Closes whatever form the player has open, if any. */
    public static void close(UUID playerId) {
        if (!isAvailable() || playerId == null) {
            return;
        }
        try {
            FloodgateApi.getInstance().closeForm(playerId);
        } catch (Exception | LinkageError e) {
            RedCraftChat.getInstance().getLogger().debug("Could not close a Bedrock form: {}", e.getMessage());
        }
    }
}
