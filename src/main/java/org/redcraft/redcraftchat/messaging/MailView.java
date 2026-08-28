package org.redcraft.redcraftchat.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.redcraft.redcraftchat.models.players.PlayerMail;

/**
 * What a player is currently looking at in their inbox.
 *
 * The inbox used to be navigable only by clicking, and the only handle on a
 * single mail was its internal id, which was never printed: it lived inside
 * the click event. That made marking one mail read unreachable for anybody
 * without a mouse, and unreachable full stop on Bedrock.
 *
 * A slot is the short number shown against a row. It is per player and per
 * page, so it is typeable, completable, and stable for as long as the page
 * on screen is the page in hand. The internal id stays the storage key.
 */
public final class MailView {

    /** Rows per page. Slots are 1..PAGE_SIZE, which is what makes them short. */
    public static final int PAGE_SIZE = 5;

    private static final Map<UUID, MailView> VIEWS = new ConcurrentHashMap<>();

    private int page = 1;
    private boolean unreadOnly = true;
    private final List<String> slots = new ArrayList<>();

    public static MailView of(UUID playerId) {
        return VIEWS.computeIfAbsent(playerId, id -> new MailView());
    }

    /** Dropped on disconnect: a slot means nothing once the page is gone. */
    public static void forget(UUID playerId) {
        VIEWS.remove(playerId);
    }

    public synchronized int page() {
        return page;
    }

    public synchronized boolean unreadOnly() {
        return unreadOnly;
    }

    /** Records the page being shown, so slots resolve against what is on screen. */
    public synchronized void record(int page, boolean unreadOnly, List<PlayerMail> shown) {
        this.page = page;
        this.unreadOnly = unreadOnly;
        slots.clear();
        for (PlayerMail mail : shown) {
            slots.add(mail.internalId);
        }
    }

    /**
     * The internal id behind a slot, or null when that slot is not on screen.
     *
     * Deliberately not clamped. A slot from a page the player has since left
     * has to miss rather than silently resolve to whatever now occupies that
     * row, because the actions behind it mark mail read and send replies.
     */
    public synchronized String internalId(int slot) {
        if (slot < 1 || slot > slots.size()) {
            return null;
        }
        return slots.get(slot - 1);
    }

    public synchronized int size() {
        return slots.size();
    }

    /** The slot numbers currently on screen, for tab completion. */
    public synchronized List<String> slotNumbers() {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= slots.size(); i++) {
            out.add(String.valueOf(i));
        }
        return out;
    }
}
