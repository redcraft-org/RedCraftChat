package org.redcraft.redcraftchat.messaging;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;

/**
 * Asks the player for a line of text, using chat as the keyboard.
 *
 * The in-world panel has no text field. DisplayKit's TextInput seam exists for
 * this and is stubbed on the proxy platform, and there is no good way to type
 * into a display entity, so the panel hands the question to chat: the next
 * thing the player types is the answer, and it is swallowed rather than sent.
 *
 * Bedrock never comes through here. Its forms have a real input box, which is
 * one of the few places Bedrock has the better interface.
 */
public final class ChatPrompt {

    /** Long enough to think, short enough that a forgotten prompt clears. */
    private static final long TIMEOUT_SECONDS = 120;

    private static final Map<UUID, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    private ChatPrompt() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * Waits for one line from this player.
     *
     * Replaces any prompt already outstanding, so a player who opens a second
     * one is answering the second rather than silently feeding the first.
     */
    public static void await(Player player, Consumer<String> answer) {
        UUID playerId = player.getUniqueId();
        PENDING.put(playerId, answer);

        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            // Identity-checked: a prompt opened after this one must not be
            // cancelled by this one's timer
            PENDING.remove(playerId, answer);
        }).delay(TIMEOUT_SECONDS, TimeUnit.SECONDS).schedule();
    }

    /**
     * Feeds a chat line to a waiting prompt.
     *
     * @return true when the line was consumed and must not be broadcast.
     */
    public static boolean offer(Player player, String message) {
        Consumer<String> answer = PENDING.remove(player.getUniqueId());
        if (answer == null) {
            return false;
        }
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        RedCraftChat plugin = RedCraftChat.getInstance();
        // Off the chat thread: answering usually writes to the database
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                answer.accept(trimmed);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Chat prompt answer failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
        return true;
    }

    public static void cancel(UUID playerId) {
        PENDING.remove(playerId);
    }

    public static boolean isWaiting(UUID playerId) {
        return PENDING.containsKey(playerId);
    }
}
