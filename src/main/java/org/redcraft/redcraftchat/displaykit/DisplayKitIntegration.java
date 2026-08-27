package org.redcraft.redcraftchat.displaykit;

import com.github.retrooper.packetevents.PacketEvents;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;

import io.schemat.displaykit.velocity.VelocityDisplayKit;
import io.schemat.displaykit.velocity.VelocityDisplayKitConfig;

/**
 * Owns the DisplayKit library lifecycle inside the plugin.
 *
 * Failure is a mode, not an exception: when the library cannot initialise,
 * isAvailable() turns false and every selector entry point falls back to the
 * chat menu. The plugin must never fail to boot over its fanciest UI.
 */
public class DisplayKitIntegration {

    private static VelocityDisplayKit displayKit = null;

    private DisplayKitIntegration() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * Called between packet listener registration and PacketEvents init, so
     * the library's input listener joins the same registration window.
     */
    public static void init(ProxyServer proxy) {
        if (!Config.displaykitSelectorEnabled) {
            return;
        }

        try {
            displayKit = VelocityDisplayKit.init(new VelocityDisplayKitConfig(
                    proxy,
                    PacketEvents.getAPI(),
                    java.util.logging.Logger.getLogger("RedCraftChat/DisplayKit"),
                    false));
        } catch (Exception | LinkageError e) {
            displayKit = null;
            RedCraftChat.getInstance().getLogger().error(
                    "DisplayKit failed to initialise, the language selector falls back to chat: {}",
                    e.getMessage());
        }
    }

    public static void shutdown() {
        if (displayKit != null) {
            displayKit = null;
            try {
                VelocityDisplayKit.shutdown();
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("DisplayKit shutdown failed: {}", e.getMessage());
            }
        }
    }

    public static boolean isAvailable() {
        return displayKit != null;
    }

    public static VelocityDisplayKit get() {
        return displayKit;
    }

    /** Whether this player's client can render a DisplayKit surface at all. */
    public static boolean isSupported(Player player) {
        return displayKit != null && displayKit.getVersionGate().isSupported(player);
    }
}
