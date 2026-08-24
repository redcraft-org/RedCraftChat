package org.redcraft.redcraftchat.listeners.packets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.translate.TranslationManager;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Translates the system chat a player receives, in place of the netty handler
 * the BungeeCord version injected into the backend connection.
 *
 * PacketEvents only sees the player facing channel on Velocity, so the packet is
 * caught on its way out: the original is dropped and a translated copy is sent
 * back to that player once the translation completed off the netty thread.
 */
public class SystemChatInterceptor extends PacketListenerAbstract {

    static TranslationManager translationManager = new TranslationManager(Config.upstreamTranslationProvider);

    /**
     * RedCraftChat sends its own join and leave messages, forwarding these would
     * show them twice.
     */
    private static final List<String> SKIPPED_TRANSLATION_KEYS = Arrays.asList(
        "multiplayer.player.joined",
        "multiplayer.player.left"
    );

    /**
     * One chain of translations per player, so the messages leave in the order
     * they arrived. The BungeeCord version fired every message off on its own
     * task and lost the order, which is visible on anything multi line.
     */
    private static final Map<UUID, CompletableFuture<Void>> pendingMessages = new ConcurrentHashMap<>();

    public SystemChatInterceptor() {
        super(PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            return;
        }

        Object receiver = event.getPlayer();
        if (!(receiver instanceof Player)) {
            // No player attached to the connection yet, leave the packet alone
            return;
        }

        Player player = (Player) receiver;
        WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
        Component message = wrapper.getMessage();

        // On 1.19.1 and later the position is this boolean, true meaning action bar
        boolean overlay = wrapper.isOverlay();

        if (message instanceof TranslatableComponent
                && SKIPPED_TRANSLATION_KEYS.contains(((TranslatableComponent) message).key())) {
            event.setCancelled(true);
            return;
        }

        // Translating blocks, so the original never reaches the client and a copy
        // is emitted from a proxy thread instead
        event.setCancelled(true);

        User user = event.getUser();
        enqueue(player, () -> handleChatPacket(user, player, message, overlay));
    }

    private void enqueue(Player player, Runnable task) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        UUID playerUniqueId = player.getUniqueId();
        Executor executor = runnable -> plugin.getProxy().getScheduler().buildTask(plugin, runnable).schedule();

        CompletableFuture<Void> queued = pendingMessages.compute(playerUniqueId, (key, previous) -> {
            CompletableFuture<Void> base = previous == null ? CompletableFuture.completedFuture(null) : previous;
            // handle() swallows a failed predecessor, one broken message must not
            // stall every message after it
            return base.handle((result, error) -> null).thenRunAsync(task, executor);
        });

        // Drop the entry again once this was the last message queued for that player
        queued.whenComplete((result, error) -> pendingMessages.remove(playerUniqueId, queued));
    }

    public static void handleChatPacket(User user, Player player, Component message, boolean overlay) {
        Component translatedMessageComponent = message;

        // Translating serialises the component down to a legacy string, which
        // keeps the colours and drops everything else. A menu that went through
        // that came out unclickable, so anything carrying a click is forwarded
        // untouched. RedCraftChat builds its own menus in the player's language
        // already, so nothing is lost by leaving them alone.
        if (isInteractive(message)) {
            sendMessage(user, player, message, overlay);
            return;
        }

        // Anything else than plain text is forwarded as it came
        if (message instanceof TextComponent) {
            LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
            String originalMessage = serializer.serialize(message);

            try {
                String sourceLanguage = DetectionManager.getLanguage(originalMessage);

                if (!PlayerPreferencesManager.playerSpeaksLanguage(player, sourceLanguage)) {
                    String targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(player);
                    if (sourceLanguage != null && !sourceLanguage.equalsIgnoreCase(targetLanguage)) {
                        String translatedMessage = translationManager.translate(originalMessage, sourceLanguage, targetLanguage);

                        // The original text only hovers over messages that were
                        // actually translated
                        translatedMessageComponent = serializer.deserialize(translatedMessage)
                                .hoverEvent(HoverEvent.showText(serializer.deserialize(originalMessage)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                String messageTemplate = "Error while translating message [%s -> %s] %s";
                String debugMessage = String.format(messageTemplate, MinecraftDiscordBridge.getServerName(player),
                        player.getUsername(), originalMessage);
                RedCraftChat.getInstance().getLogger().error(debugMessage);
            }
        }

        sendMessage(user, player, translatedMessageComponent, overlay);
    }

    private static void sendMessage(User user, Player player, Component message, boolean overlay) {
        try {
            // Silently, otherwise the packet goes back through the packetevents
            // encoder and this listener sees its own message again
            user.sendPacketSilently(new WrapperPlayServerSystemChatMessage(overlay, message));
        } catch (Exception e) {
            String messageTemplate = "Encountered an exception while parsing incoming message from server %s to player %s: %s";
            String errorMessage = String.format(messageTemplate, MinecraftDiscordBridge.getServerName(player),
                    player.getUsername(), e.getMessage());
            RedCraftChat.getInstance().getLogger().error(errorMessage);
            e.printStackTrace();
        }
    }

    /**
     * A click anywhere in the tree makes the whole message interactive, since
     * serialising any part of it would drop that child's event.
     */
    private static boolean isInteractive(Component message) {
        if (message.clickEvent() != null) {
            return true;
        }

        for (Component child : message.children()) {
            if (isInteractive(child)) {
                return true;
            }
        }

        return false;
    }
}
