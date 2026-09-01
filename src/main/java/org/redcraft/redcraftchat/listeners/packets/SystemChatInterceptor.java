package org.redcraft.redcraftchat.listeners.packets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.translate.LineBlock;
import org.redcraft.redcraftchat.translate.TranslationManager;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.ConnectionState;
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
 *
 * A plugin printing a menu, a help page or a multi line announcement sends one
 * packet per line, and a line pulled out of that block is a poor thing to
 * translate: it has lost the sentence it belonged to, and a short one carries
 * too little to even detect a language on. Messages arriving within
 * upstream-chat-grouping-delay of each other are therefore collected and
 * translated as one block, then handed back out one packet at a time in the
 * order they arrived. That delay is what every system message now waits before
 * the player sees it, which is why it belongs in the config and is measured in
 * milliseconds rather than ticks.
 *
 * Only plain chat joins a block. Action bar text is a different surface that
 * happens to share this packet, so it is translated on its own, and anything
 * clickable is forwarded untouched, since serialising it down to legacy text
 * would drop the click.
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

    /** Messages waiting for their block to fill, one buffer per player. */
    private static final Map<UUID, Batch> batches = new ConcurrentHashMap<>();

    private static class Batch {
        final List<BufferedMessage> messages = new ArrayList<>();
        boolean flushScheduled;
    }

    /**
     * One captured packet, with the decision about how it may be translated
     * already taken: the classification reads the component, which is work
     * that does not belong on the netty thread twice.
     */
    public static class BufferedMessage {
        public final Component message;
        public final boolean overlay;

        /** The text to translate, null when this message is forwarded as is. */
        public final String legacy;

        /** Whether it may share a block with the other messages. */
        public final boolean groupable;

        public BufferedMessage(Component message, boolean overlay) {
            this.message = message;
            this.overlay = overlay;

            String text = null;
            if (message instanceof TextComponent && !isInteractive(message)) {
                String serialized = LegacyComponentSerializer.legacySection().serialize(message);
                if (LineBlock.hasTranslatableText(serialized)) {
                    text = serialized;
                }
            }

            this.legacy = text;
            // Action bar text is its own display, blocking it together with
            // chat would translate two unrelated things as one sentence
            this.groupable = text != null && !overlay;
        }
    }

    /**
     * ActionBarTranslator owns both wire forms of the action bar and rewrites
     * in place, so an overlay message is neither cancelled nor held here when
     * that path is on. It also stops paying the grouping delay and a
     * scheduler hop for a surface that repaints itself anyway. With the flag
     * off, overlay messages take the buffer exactly as before, which is the
     * rollback lever.
     */
    public static boolean leaveToActionBarPath(boolean overlay) {
        return overlay && Config.translateActionBars;
    }

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

        if (leaveToActionBarPath(overlay)) {
            return;
        }

        if (message instanceof TranslatableComponent
                && SKIPPED_TRANSLATION_KEYS.contains(((TranslatableComponent) message).key())) {
            event.setCancelled(true);
            return;
        }

        // Translating blocks, so the original never reaches the client and a copy
        // is emitted from a proxy thread instead
        event.setCancelled(true);

        buffer(event.getUser(), player, new BufferedMessage(message, overlay));
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID playerUniqueId = event.getUser().getProfile().getUUID();
        if (playerUniqueId != null) {
            batches.remove(playerUniqueId);
        }
    }

    /**
     * Adds a message to its player's block and, if this opened the block,
     * schedules the flush that will close it. Only the message that opened it
     * schedules, so the window runs from the first message rather than being
     * pushed back by every one that follows.
     */
    private void buffer(User user, Player player, BufferedMessage message) {
        Batch batch = batches.computeIfAbsent(player.getUniqueId(), id -> new Batch());

        boolean opened;
        synchronized (batch) {
            batch.messages.add(message);
            opened = !batch.flushScheduled;
            batch.flushScheduled = true;
        }

        if (!opened) {
            return;
        }

        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            List<BufferedMessage> drained = drain(player.getUniqueId());
            if (!drained.isEmpty()) {
                // The chain, not the scheduler, decides when this batch is
                // sent: its own translation may outlast the next batch's
                enqueue(player, () -> handleBatch(user, player, drained));
            }
        }).delay(Math.max(0, Config.upstreamChatGroupingDelay), TimeUnit.MILLISECONDS).schedule();
    }

    /**
     * Empties the block in place rather than dropping it from the map: a
     * message being buffered right now holds this same object, and taking it
     * away would lose that message with nothing left to schedule its flush.
     * Clearing the flag under the same lock instead means such a message finds
     * an empty block and opens the next one itself.
     */
    private static List<BufferedMessage> drain(UUID playerUniqueId) {
        Batch batch = batches.get(playerUniqueId);
        if (batch == null) {
            return Collections.emptyList();
        }

        synchronized (batch) {
            List<BufferedMessage> drained = new ArrayList<>(batch.messages);
            batch.messages.clear();
            batch.flushScheduled = false;
            return drained;
        }
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

    /**
     * Translates a batch and sends every message of it, in arrival order. The
     * groupable ones go to the provider as one block, the rest keep the one at
     * a time behaviour.
     */
    static void handleBatch(User user, Player player, List<BufferedMessage> batch) {
        Map<String, String> translated = translateLines(player, blockOf(batch));

        for (BufferedMessage buffered : batch) {
            Component message = buffered.message;

            if (buffered.legacy != null) {
                String result = buffered.groupable
                        ? translated.get(buffered.legacy)
                        : translateLines(player, Collections.singletonList(buffered.legacy)).get(buffered.legacy);

                if (result != null && !result.equals(buffered.legacy)) {
                    LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
                    // The original text only hovers over messages that were
                    // actually translated
                    message = serializer.deserialize(result)
                            .hoverEvent(HoverEvent.showText(serializer.deserialize(buffered.legacy)));
                }
            }

            sendMessage(user, player, message, buffered.overlay);
        }
    }

    /** The lines of a batch that are translated together, in arrival order. */
    public static List<String> blockOf(List<BufferedMessage> batch) {
        List<String> block = new ArrayList<>();
        for (BufferedMessage buffered : batch) {
            if (buffered.groupable) {
                block.add(buffered.legacy);
            }
        }
        return block;
    }

    /**
     * Translates lines that belong together, keyed by their original text. The
     * language is detected on the whole block, which is the point of grouping:
     * a line reading "server!" has nothing to detect on its own. An empty map
     * means the text stays as it is, whether because the player already speaks
     * it or because the translation failed.
     */
    static Map<String, String> translateLines(Player player, List<String> lines) {
        if (lines.isEmpty()) {
            return Collections.emptyMap();
        }

        String block = LineBlock.join(lines);

        try {
            String sourceLanguage = DetectionManager.getLanguage(block);

            if (PlayerPreferencesManager.playerSpeaksLanguage(player, sourceLanguage)) {
                return Collections.emptyMap();
            }

            String targetLanguage = PlayerPreferencesManager.getMainPlayerLanguage(player);
            if (sourceLanguage == null || sourceLanguage.equalsIgnoreCase(targetLanguage)) {
                return Collections.emptyMap();
            }

            if (lines.size() == 1) {
                return Collections.singletonMap(lines.get(0),
                        translationManager.translate(block, sourceLanguage, targetLanguage));
            }

            Map<String, String> aligned = LineBlock.align(lines,
                    translationManager.translate(block, sourceLanguage, targetLanguage));

            if (aligned != null) {
                return aligned;
            }

            // The provider reshaped the block, so its lines cannot be matched
            // back to the packets they came from. Translating them one by one
            // loses the context but never shows text on the wrong line
            Map<String, String> lineByLine = new LinkedHashMap<>();
            for (String line : lines) {
                lineByLine.put(line, translationManager.translate(line, sourceLanguage, targetLanguage));
            }
            return lineByLine;
        } catch (Exception e) {
            e.printStackTrace();
            String messageTemplate = "Error while translating message [%s -> %s] %s";
            String debugMessage = String.format(messageTemplate, MinecraftDiscordBridge.getServerName(player),
                    player.getUsername(), block);
            RedCraftChat.getInstance().getLogger().error(debugMessage);
            return Collections.emptyMap();
        }
    }

    private static void sendMessage(User user, Player player, Component message, boolean overlay) {
        try {
            // Every message here is a delayed copy: the original was cancelled
            // and held for the grouping window before being written back. The
            // player can leave PLAY inside that window, by switching servers
            // or quitting, and a play packet written to a connection that has
            // moved to configuration is decoded against the wrong table. The
            // client does not recover from that, it drops the connection with
            // an unknown packet id.
            //
            // getEncoderState rather than getConnectionState: the latter
            // throws while the encoder and decoder disagree, which is exactly
            // the moment being guarded against.
            if (user.getEncoderState() != ConnectionState.PLAY) {
                return;
            }
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
    public static boolean isInteractive(Component message) {
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
