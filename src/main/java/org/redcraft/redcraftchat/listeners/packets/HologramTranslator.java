package org.redcraft.redcraftchat.listeners.packets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.translate.LineBlock;
import org.redcraft.redcraftchat.translate.TranslationManager;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Translates hologram text on its way to the player.
 *
 * DecentHolograms renders each hologram line as an invisible armor stand with
 * a custom name, sent as raw entity packets, so nothing of it ever passes the
 * chat pipeline. This listener recognises those entities from the packets
 * alone: an armor stand spawn followed by metadata carrying the invisible flag
 * and a custom name is a hologram line. Player nametags are not armor stands,
 * and decorative named armor stands in builds are visible. Other plugins'
 * floating text lines fit the same shape and get translated with them, which
 * is treated as the feature rather than a false positive.
 *
 * A translated line is rewritten inside the outgoing packet, so on a cache hit
 * the client only ever sees the translated text. The first sight of a line is
 * let through untranslated and a corrected metadata packet follows once the
 * translation lands. Results are kept in memory here and persistently by the
 * provider cache, so that first sight happens once per line and language ever,
 * not per boot.
 *
 * Translation happens a hologram at a time, not a line at a time. One hologram
 * is a column of armor stands a quarter block apart, so its lines are grouped
 * by position and sent to the provider as one block: the tail lines of a
 * sentence split across a hologram are undetectable and untranslatable on
 * their own, but obvious inside the whole text. Each line then gets its slice
 * of the result, and the cache stays per line so the netty thread can rewrite
 * a repeated sighting without the geometry.
 *
 * Lines whose text keeps changing, countdowns and animations, are detected by
 * their churn and left alone: translating every frame would burn provider
 * quota to display permanently stale text.
 *
 * The proxy speaks each client's own protocol version, ViaVersion lives on the
 * backends. Old clients therefore receive armor stands as SPAWN_LIVING_ENTITY
 * with the metadata embedded, and their custom name is a plain string or a
 * json component rather than an adventure component, which is why the name is
 * matched and rewritten by shape.
 */
public class HologramTranslator extends PacketListenerAbstract {

    private static final byte ENTITY_FLAG_INVISIBLE = 0x20;

    /**
     * A line whose text changes this many times inside the window is dynamic
     * and stops being translated. A countdown trips this within seconds, and
     * the lifetime total catches slow cyclers like a clock line, which change
     * too gently for the window but still mint one cache entry per frame.
     */
    public static final int DYNAMIC_TEXT_CHANGES = 3;
    public static final long DYNAMIC_TEXT_WINDOW_MILLIS = 10_000;
    public static final int DYNAMIC_TOTAL_CHANGES = 20;

    /**
     * How long a line sits out after a failed provider call before another
     * attempt. Without this a provider outage would retry every update tick,
     * and caching the failure instead would pin the line untranslated forever.
     */
    static final long FAILURE_RETRY_MILLIS = 60_000;

    /**
     * Hard ceiling on cached lines, a backstop behind the churn detection.
     * Translations past it are still delivered, just not remembered.
     */
    static final int MAX_CACHED_LINES = 10_000;

    /**
     * How the lines of one hologram are recognised as one hologram: they
     * stand in the same column within this horizontal tolerance, and each
     * line within LINE_SPACING_LIMIT of the next. DecentHolograms stacks
     * text lines a quarter block apart, while separate holograms stand well
     * clear of each other.
     */
    public static final double COLUMN_EPSILON = 0.05;
    public static final double LINE_SPACING_LIMIT = 0.6;

    /**
     * A first sighting waits hologram-grouping-delay before translating. The
     * sibling lines of a freshly spawned hologram arrive together, and waiting
     * for them means the hologram translates as one block instead of one
     * impoverished line at a time. Nothing is held up by the wait, the client
     * is showing the original text meanwhile either way.
     */

    static TranslationManager translationManager = new TranslationManager(Config.upstreamTranslationProvider);

    /**
     * How the client expects the name encoded, decided by its protocol
     * version. The translation itself always works on legacy text.
     */
    public enum NameShape {
        /** 1.19 and later, an adventure component */
        COMPONENT,
        /** 1.13 to 1.18.2, a json string component */
        JSON,
        /** 1.12.2 and before, a plain string with paragraph codes */
        LEGACY_STRING
    }

    /**
     * Everything needed to rebuild a line's name packet later: the refresh on
     * a language change has no metadata packet in hand to copy from.
     */
    static class LineInfo {
        final String legacy;
        final int nameIndex;
        final EntityDataType<?> nameType;
        final NameShape shape;

        LineInfo(CustomName name) {
            this.legacy = name.legacy;
            this.nameIndex = name.entry.getIndex();
            this.nameType = name.entry.getType();
            this.shape = name.shape;
        }
    }

    /** A custom name found in a metadata list, with its wire shape. */
    public static class CustomName {
        public final EntityData<?> entry;
        public final NameShape shape;
        public final String legacy;

        CustomName(EntityData<?> entry, NameShape shape, String legacy) {
            this.entry = entry;
            this.shape = shape;
            this.legacy = legacy;
        }
    }

    /**
     * Change frequency of one line's text. Not thread safe on its own, all
     * access happens under the per entity map entry.
     */
    public static class TextChurn {
        private long windowStart;
        private int changes;

        private int totalChanges;

        /** Records one text change and answers whether the line is dynamic. */
        public synchronized boolean recordChange(long now) {
            totalChanges++;
            if (totalChanges >= DYNAMIC_TOTAL_CHANGES) {
                return true;
            }
            if (now - windowStart > DYNAMIC_TEXT_WINDOW_MILLIS) {
                windowStart = now;
                changes = 0;
            }
            changes++;
            return changes >= DYNAMIC_TEXT_CHANGES;
        }
    }

    /**
     * What each connection is looking at. Entity ids are assigned by the
     * backend, so everything here is per player and reset on server switch.
     */
    private static class TrackedEntities {
        final Set<Integer> armorStands = ConcurrentHashMap.newKeySet();
        final Set<Integer> holograms = ConcurrentHashMap.newKeySet();
        final Set<Integer> dynamic = ConcurrentHashMap.newKeySet();
        final Map<Integer, LineInfo> lastText = new ConcurrentHashMap<>();
        final Map<Integer, TextChurn> churn = new ConcurrentHashMap<>();

        /** x, y, z per armor stand, the geometry the line grouping goes by. */
        final Map<Integer, double[]> positions = new ConcurrentHashMap<>();

        void forget(int entityId) {
            armorStands.remove(entityId);
            holograms.remove(entityId);
            dynamic.remove(entityId);
            lastText.remove(entityId);
            churn.remove(entityId);
            positions.remove(entityId);
        }
    }

    private final Map<User, TrackedEntities> tracked = new ConcurrentHashMap<>();

    /**
     * The languages a player wants and speaks, resolved off the netty thread
     * since the preference lookup goes through the cache backend. Dropped on
     * server switch, so a language change applies without relogging.
     */
    public static class PlayerLanguages {
        final String target;
        final Set<String> spoken;

        PlayerLanguages(String target, Set<String> spoken) {
            this.target = target;
            this.spoken = spoken;
        }
    }

    private final Map<UUID, PlayerLanguages> languageByPlayer = new ConcurrentHashMap<>();

    /**
     * A finished translation. The source language rides along so a player who
     * speaks it can be left reading the original, the way chat behaves. Null
     * source means detection had no answer, which is deterministic and safe
     * to remember, unlike a provider failure which is never cached.
     */
    static class CachedTranslation {
        final String source;
        final String translated;

        CachedTranslation(String source, String translated) {
            this.source = source;
            this.translated = translated;
        }
    }

    /**
     * language + '\0' + legacy text -> result. Identity results are stored
     * too, so a line needing no translation is never detected twice. This is
     * the only lookup the netty thread performs.
     */
    private static final Map<String, CachedTranslation> translations = new ConcurrentHashMap<>();

    /** Lines whose last provider call failed, and when, for the backoff. */
    private static final Map<String, Long> failedAt = new ConcurrentHashMap<>();

    /** Backstop for the failure map, cleared whole when it fills. */
    static final int MAX_FAILED_LINES = 1_000;

    /** Serialises language resolution per player, see scheduleTranslation. */
    private final Map<UUID, Object> resolveLocks = new ConcurrentHashMap<>();

    /**
     * In flight translations, so ten players staring at the same hologram cost
     * one provider call. Waiters receive a corrected packet on completion.
     */
    private final Map<String, PendingTranslation> pending = new ConcurrentHashMap<>();

    private static class PendingTranslation {
        final List<Waiter> waiters = new CopyOnWriteArrayList<>();

        /**
         * Set by the owner before the pending entry is removed, so a late
         * joiner can deliver itself even when cache admission declined the
         * result. Keyed by source line, since the pending unit is the whole
         * hologram and each waiter only wants its own line back.
         */
        volatile Map<String, CachedTranslation> results;
    }

    private static class Waiter {
        final User user;
        final UUID playerId;
        final int entityId;
        final int nameIndex;
        final EntityDataType<?> nameType;
        final NameShape shape;
        final String originalText;

        /**
         * A refresh waiter restores the original text when the new language
         * needs no translation, since the client may still be displaying the
         * previous language. Packet driven waiters never do: their client is
         * already showing the original.
         */
        final boolean restore;

        Waiter(User user, UUID playerId, int entityId, LineInfo line, boolean restore) {
            this.user = user;
            this.playerId = playerId;
            this.entityId = entityId;
            this.nameIndex = line.nameIndex;
            this.nameType = line.nameType;
            this.shape = line.shape;
            this.originalText = line.legacy;
            this.restore = restore;
        }
    }

    /** The registered listener, so a preference update can reach it. */
    private static volatile HologramTranslator instance;

    public HologramTranslator() {
        super(PacketListenerPriority.NORMAL);
        instance = this;
    }

    /**
     * Called whenever preferences are saved. Drops the cached language and
     * pushes corrected packets for every hologram line the player currently
     * sees, since holograms only ever resend on their own when they change.
     */
    public static void onPreferencesUpdated(UUID playerId) {
        HologramTranslator translator = instance;
        if (translator != null && playerId != null) {
            translator.refreshPlayer(playerId);
        }
    }

    private void refreshPlayer(UUID playerId) {
        languageByPlayer.remove(playerId);

        Player player = RedCraftChat.getInstance().getProxy().getPlayer(playerId).orElse(null);
        if (player == null) {
            return;
        }

        for (Map.Entry<User, TrackedEntities> entry : tracked.entrySet()) {
            UUID uuid = entry.getKey().getProfile().getUUID();
            if (!playerId.equals(uuid)) {
                continue;
            }

            TrackedEntities state = entry.getValue();
            for (Map.Entry<Integer, LineInfo> line : state.lastText.entrySet()) {
                int entityId = line.getKey();
                if (!state.holograms.contains(entityId) || state.dynamic.contains(entityId)
                        || !hasTranslatableText(line.getValue().legacy)) {
                    continue;
                }
                scheduleTranslation(
                        new Waiter(entry.getKey(), playerId, entityId, line.getValue(), true), player);
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            onSpawn(event);
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            onSpawnLiving(event);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            onMetadata(event);
        } else if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            onDestroy(event);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            onTeleport(event);
        } else if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME
                || event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            // Server switch: every entity id belongs to the old backend now
            tracked.remove(event.getUser());
            Object receiver = event.getPlayer();
            if (receiver instanceof Player) {
                languageByPlayer.remove(((Player) receiver).getUniqueId());
            }
            event.markForReEncode(false);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        tracked.remove(event.getUser());
        UUID uuid = event.getUser().getProfile().getUUID();
        if (uuid != null) {
            languageByPlayer.remove(uuid);
            resolveLocks.remove(uuid);
        }
    }

    private void onSpawn(PacketSendEvent event) {
        WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);

        if (wrapper.getEntityType() == EntityTypes.ARMOR_STAND) {
            TrackedEntities state = trackedFor(event);
            state.armorStands.add(wrapper.getEntityId());
            state.positions.put(wrapper.getEntityId(), toPosition(wrapper.getPosition()));
        }

        event.markForReEncode(false);
    }

    /**
     * Clients before 1.19 receive armor stands through this packet, and up to
     * 1.14.4 the first metadata rides inside it, so it goes through the same
     * processing and may be rewritten in place.
     */
    private void onSpawnLiving(PacketSendEvent event) {
        WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);

        if (wrapper.getEntityType() != EntityTypes.ARMOR_STAND) {
            event.markForReEncode(false);
            return;
        }

        TrackedEntities state = trackedFor(event);
        state.armorStands.add(wrapper.getEntityId());
        state.positions.put(wrapper.getEntityId(), toPosition(wrapper.getPosition()));

        List<EntityData<?>> metadata = wrapper.getEntityMetadata();
        if (metadata == null || metadata.isEmpty()) {
            event.markForReEncode(false);
            return;
        }

        handleHologramMetadata(event, wrapper.getEntityId(), metadata);
    }

    private void onMetadata(PacketSendEvent event) {
        if (!tracked.containsKey(event.getUser())) {
            return;
        }

        WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
        handleHologramMetadata(event, wrapper.getEntityId(), wrapper.getEntityMetadata());
    }

    /**
     * A hologram moves as a whole when repositioned, and the grouping goes by
     * position, so a teleported line keeps its recorded spot current.
     */
    private void onTeleport(PacketSendEvent event) {
        TrackedEntities state = tracked.get(event.getUser());

        if (state != null) {
            WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(event);
            if (state.armorStands.contains(wrapper.getEntityId())) {
                state.positions.put(wrapper.getEntityId(), toPosition(wrapper.getPosition()));
            }
        }

        event.markForReEncode(false);
    }

    private static double[] toPosition(Vector3d position) {
        return new double[] { position.getX(), position.getY(), position.getZ() };
    }

    private void onDestroy(PacketSendEvent event) {
        TrackedEntities state = tracked.get(event.getUser());

        if (state != null) {
            WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(event);
            for (int entityId : wrapper.getEntityIds()) {
                state.forget(entityId);
            }
        }

        event.markForReEncode(false);
    }

    private TrackedEntities trackedFor(PacketSendEvent event) {
        return tracked.computeIfAbsent(event.getUser(), user -> new TrackedEntities());
    }

    /**
     * The netty thread path. Everything here is in memory: the single lookup
     * that can rewrite the packet is a map hit, and every miss is handed to
     * the scheduler.
     */
    private void handleHologramMetadata(PacketSendEvent event, int entityId, List<EntityData<?>> metadata) {
        TrackedEntities state = tracked.get(event.getUser());
        if (state == null) {
            event.markForReEncode(false);
            return;
        }

        boolean known = state.holograms.contains(entityId);
        if (!known && !state.armorStands.contains(entityId)) {
            event.markForReEncode(false);
            return;
        }

        CustomName name = findCustomName(metadata);

        if (!known) {
            // Promotion needs the full picture: the invisible flag and the name
            // arrive together in the first metadata the client gets
            if (name == null || !isInvisible(metadata)) {
                event.markForReEncode(false);
                return;
            }
            state.holograms.add(entityId);
        }

        if (name == null) {
            // A flags or pose only update on a known hologram line
            event.markForReEncode(false);
            return;
        }

        // The current text is recorded before any early return: it is the
        // staleness guard for corrective packets, and a change to letterless
        // text still has to invalidate an in flight translation of the
        // previous content
        LineInfo line = new LineInfo(name);
        LineInfo previous = state.lastText.put(entityId, line);
        if (previous != null && !previous.legacy.equals(name.legacy)) {
            TextChurn churn = state.churn.computeIfAbsent(entityId, id -> new TextChurn());
            if (churn.recordChange(System.currentTimeMillis())) {
                state.dynamic.add(entityId);
            }
        }

        if (!hasTranslatableText(name.legacy)) {
            // Decorative lines, arrows and dividers have nothing to translate
            event.markForReEncode(false);
            return;
        }

        if (state.dynamic.contains(entityId)) {
            event.markForReEncode(false);
            return;
        }

        Object receiver = event.getPlayer();
        if (!(receiver instanceof Player)) {
            event.markForReEncode(false);
            return;
        }
        Player player = (Player) receiver;

        PlayerLanguages languages = languageByPlayer.get(player.getUniqueId());
        CachedTranslation cached = languages == null ? null
                : translations.get(cacheKey(languages.target, name.legacy));

        if (cached == null) {
            // First sight: the original text is a fine placeholder for the
            // moment it takes, a corrected packet follows the translation
            scheduleTranslation(new Waiter(event.getUser(), player.getUniqueId(), entityId, line, false), player);
            event.markForReEncode(false);
            return;
        }

        if (!shouldRewrite(cached, name.legacy, languages)) {
            event.markForReEncode(false);
            return;
        }

        setValue(name.entry, encodeName(name.shape, cached.translated));
        event.markForReEncode(true);
    }

    /**
     * Identity results never rewrite, and a player who speaks the source
     * language keeps the original, the way chat leaves their messages alone.
     */
    private static boolean shouldRewrite(CachedTranslation cached, String original, PlayerLanguages languages) {
        if (cached.translated.equals(original)) {
            return false;
        }

        return cached.source == null || !languages.spoken.contains(cached.source);
    }

    private void scheduleTranslation(Waiter waiter, Player player) {
        RedCraftChat plugin = RedCraftChat.getInstance();

        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            // Not computeIfAbsent: the resolve blocks on the preference
            // backend, and a compute would hold the map bin hostage for its
            // whole duration. The per player lock instead, because the first
            // sight of a multi line hologram schedules one task per line and
            // they would all resolve at once, racing the provider's own
            // check-then-insert into creating duplicate player rows.
            PlayerLanguages languages = languageByPlayer.get(waiter.playerId);
            if (languages == null) {
                Object lock = resolveLocks.computeIfAbsent(waiter.playerId, id -> new Object());
                synchronized (lock) {
                    languages = languageByPlayer.get(waiter.playerId);
                    if (languages == null) {
                        languages = resolveLanguages(player);
                        if (languages == null) {
                            // Preference backend failure: never cached, the
                            // next sighting tries again instead of pinning a
                            // wrong language for the whole session
                            return;
                        }
                        languageByPlayer.put(waiter.playerId, languages);
                        if (!player.isActive()) {
                            // The put raced the disconnect cleanup, undo it
                            languageByPlayer.remove(waiter.playerId);
                            return;
                        }
                    }
                }
            }

            CachedTranslation cached = translations.get(cacheKey(languages.target, waiter.originalText));
            if (cached != null) {
                deliver(waiter, cached);
                return;
            }

            // The waiter's whole hologram is the translation unit, so the
            // model sees every line as context for every other: the tail of
            // a sentence split across lines is untranslatable alone
            List<String> groupLines = contextLines(waiter);
            String key = cacheKey(languages.target, LineBlock.join(groupLines));

            Long lastFailure = failedAt.get(key);
            if (lastFailure != null && System.currentTimeMillis() - lastFailure < FAILURE_RETRY_MILLIS) {
                return;
            }

            PendingTranslation mine = new PendingTranslation();
            PendingTranslation current = pending.putIfAbsent(key, mine);
            if (current != null) {
                current.waiters.add(waiter);
                // The owner may have flushed between the get above and this
                // add. Its results field is set before the flush, so reading
                // it here closes the window whatever the cache admission
                // decided
                Map<String, CachedTranslation> flushed = current.results;
                if (flushed != null) {
                    CachedTranslation late = flushed.get(waiter.originalText);
                    if (late != null) {
                        deliver(waiter, late);
                    }
                }
                return;
            }
            mine.waiters.add(waiter);

            // Winning ownership right after the previous owner finished means
            // the results already exist, take them rather than buying a
            // second provider call
            Map<String, CachedTranslation> results = cachedGroup(groupLines, languages.target);

            if (results == null) {
                try {
                    results = translateGroup(groupLines, languages.target);
                } finally {
                    if (results == null) {
                        // Provider failure or an unexpected throw: never
                        // cached, and the key must not stay wedged in the
                        // pending map or the group would be dead until restart
                        failedAt.put(key, System.currentTimeMillis());
                        if (failedAt.size() > MAX_FAILED_LINES) {
                            // Crude but bounded, the worst case is a retry
                            // arriving early
                            failedAt.clear();
                        }
                        pending.remove(key);
                    }
                }
                if (results == null) {
                    return;
                }

                failedAt.remove(key);
                for (Map.Entry<String, CachedTranslation> entry : results.entrySet()) {
                    String lineKey = cacheKey(languages.target, entry.getKey());
                    if (translations.size() < MAX_CACHED_LINES || translations.containsKey(lineKey)) {
                        translations.put(lineKey, entry.getValue());
                    }
                }
            }

            mine.results = results;

            PendingTranslation finished = pending.remove(key);
            if (finished != null) {
                for (Waiter queued : finished.waiters) {
                    CachedTranslation result = results.get(queued.originalText);
                    if (result != null) {
                        deliver(queued, result);
                    }
                }
            }
        }).delay(Math.max(0, Config.hologramGroupingDelay), TimeUnit.MILLISECONDS).schedule();
    }

    /**
     * The lines of the hologram the waiter's line belongs to, top first,
     * gathered by geometry from what the player currently sees. Dynamic lines
     * are left out because their churn would make the group key unstable, and
     * letterless decoration because it has nothing to contribute. The line
     * itself always rides along, its waiter's text rather than the tracked
     * one: if the line moved on since scheduling, the delivery guard drops
     * the result anyway, but the group has to contain the text being waited
     * on. Any missing geometry degrades to the line alone, the old behaviour.
     */
    private List<String> contextLines(Waiter waiter) {
        TrackedEntities state = tracked.get(waiter.user);
        if (state == null || waiter.originalText.indexOf('\n') >= 0) {
            return Collections.singletonList(waiter.originalText);
        }

        Map<Integer, String> texts = new HashMap<>();
        for (int entityId : state.holograms) {
            LineInfo line = state.lastText.get(entityId);
            if (line == null || state.dynamic.contains(entityId)
                    || !hasTranslatableText(line.legacy) || line.legacy.indexOf('\n') >= 0) {
                continue;
            }
            texts.put(entityId, line.legacy);
        }
        texts.put(waiter.entityId, waiter.originalText);

        List<String> group = groupHologramLines(waiter.entityId, state.positions, texts);
        return group == null ? Collections.singletonList(waiter.originalText) : group;
    }

    /**
     * One hologram's worth of lines around the given entity, top line first:
     * the lines standing in the same column, chained while each is within
     * LINE_SPACING_LIMIT of the next. Null when the entity's own geometry is
     * missing.
     */
    public static List<String> groupHologramLines(int entityId, Map<Integer, double[]> positions,
            Map<Integer, String> texts) {
        double[] center = positions.get(entityId);
        if (center == null || !texts.containsKey(entityId)) {
            return null;
        }

        // y and entity id per line of the column, sorted top first
        List<double[]> column = new ArrayList<>();
        for (Integer id : texts.keySet()) {
            double[] position = positions.get(id);
            if (position == null
                    || Math.abs(position[0] - center[0]) > COLUMN_EPSILON
                    || Math.abs(position[2] - center[2]) > COLUMN_EPSILON) {
                continue;
            }
            column.add(new double[] { position[1], id });
        }
        column.sort((a, b) -> Double.compare(b[0], a[0]));

        int own = 0;
        while ((int) column.get(own)[1] != entityId) {
            own++;
        }

        int first = own;
        while (first > 0 && column.get(first - 1)[0] - column.get(first)[0] <= LINE_SPACING_LIMIT) {
            first--;
        }
        int last = own;
        while (last < column.size() - 1 && column.get(last)[0] - column.get(last + 1)[0] <= LINE_SPACING_LIMIT) {
            last++;
        }

        List<String> group = new ArrayList<>();
        for (int i = first; i <= last; i++) {
            group.add(texts.get((int) column.get(i)[1]));
        }
        return group;
    }

    /** The group rebuilt from cache alone, null unless every line is there. */
    private static Map<String, CachedTranslation> cachedGroup(List<String> lines, String targetLanguage) {
        Map<String, CachedTranslation> results = new LinkedHashMap<>();
        for (String line : lines) {
            CachedTranslation cached = translations.get(cacheKey(targetLanguage, line));
            if (cached == null) {
                return null;
            }
            results.put(line, cached);
        }
        return results;
    }

    /**
     * Translates a hologram as one block and hands each line its slice back.
     * A single line keeps the old behaviour verbatim, and a provider that
     * reshapes the block instead of keeping its line structure falls back to
     * line by line calls. Null on provider failure, so the caller backs the
     * whole group off as one unit.
     */
    static Map<String, CachedTranslation> translateGroup(List<String> sourceLines, String targetLanguage) {
        CachedTranslation block = translate(LineBlock.join(sourceLines), targetLanguage);
        if (block == null) {
            return null;
        }

        if (sourceLines.size() == 1) {
            return Collections.singletonMap(sourceLines.get(0), block);
        }

        Map<String, String> aligned = LineBlock.align(sourceLines, block.translated);

        if (aligned == null) {
            Map<String, CachedTranslation> lineByLine = new LinkedHashMap<>();
            for (String line : sourceLines) {
                CachedTranslation result = translate(line, targetLanguage);
                if (result == null) {
                    return null;
                }
                lineByLine.put(line, result);
            }
            return lineByLine;
        }

        Map<String, CachedTranslation> results = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : aligned.entrySet()) {
            results.put(entry.getKey(), new CachedTranslation(block.source, entry.getValue()));
        }
        return results;
    }

    public static Map<String, String> alignGroupTranslation(List<String> sourceLines, String translatedBlock) {
        return LineBlock.align(sourceLines, translatedBlock);
    }

    /**
     * Normalised to bare ISO 639-1 codes. The preference path already returns
     * one for the main language, but its fallback hands back a full locale
     * like fr-FR, which would defeat the identity check against the detector
     * and split the cache. A player without preferences gets the proxy default
     * and an empty spoken list.
     */
    static PlayerLanguages resolveLanguages(Player player) {
        String target = null;
        Set<String> spoken = new HashSet<>();

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            if (preferences != null) {
                target = preferences.mainLanguage;
                if (preferences.languages != null) {
                    for (String language : preferences.languages) {
                        spoken.add(language.split("-")[0].toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            // Null rather than a default: caching a guessed language for the
            // whole session over one backend hiccup is the same failure mode
            // the provider backoff exists to prevent
            RedCraftChat.getInstance().getLogger().warn(
                    String.format("Could not resolve languages for %s, will retry", player.getUsername()));
            return null;
        }

        if (target == null) {
            target = Config.defaultLocale;
        }

        return new PlayerLanguages(target.split("-")[0].toLowerCase(), spoken);
    }

    /**
     * The source language is detected the way chat does it. Returns null on a
     * provider failure so the caller can back off instead of caching it, while
     * a line detection has no answer for is a stable identity result.
     */
    static CachedTranslation translate(String legacy, String targetLanguage) {
        try {
            // Detection sits inside the try: its lazy model build can throw
            // too, and an escaped exception here would wedge the pending key
            String sourceLanguage = DetectionManager.getLanguage(legacy);

            if (sourceLanguage == null) {
                return new CachedTranslation(null, legacy);
            }

            if (sourceLanguage.equalsIgnoreCase(targetLanguage)) {
                return new CachedTranslation(sourceLanguage.toLowerCase(), legacy);
            }

            String translated = translationManager.translate(legacy, sourceLanguage, targetLanguage);
            return new CachedTranslation(sourceLanguage.toLowerCase(), translated);
        } catch (InterruptedException e) {
            // A shutdown is not a provider failure, put the flag back
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().error(
                    String.format("Error while translating hologram line [-> %s] %s", targetLanguage, legacy), e);
            return null;
        }
    }

    private void deliver(Waiter waiter, CachedTranslation cached) {
        PlayerLanguages languages = languageByPlayer.get(waiter.playerId);
        if (languages == null) {
            return;
        }

        String text;
        if (shouldRewrite(cached, waiter.originalText, languages)) {
            text = cached.translated;
        } else if (waiter.restore) {
            // The client may still display the previous language, put the
            // original back
            text = waiter.originalText;
        } else {
            return;
        }

        TrackedEntities state = tracked.get(waiter.user);
        if (state == null || !state.holograms.contains(waiter.entityId)) {
            // Disconnected or switched server while the translation ran
            return;
        }

        LineInfo current = state.lastText.get(waiter.entityId);
        if (current == null || !waiter.originalText.equals(current.legacy)) {
            // The line moved on while the translation was in flight, a
            // corrective packet now would paste stale text over newer content
            return;
        }

        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityData<?> nameEntry = new EntityData(waiter.nameIndex, (EntityDataType) waiter.nameType,
                    encodeName(waiter.shape, text));

            // Silently, so this listener never sees its own correction
            waiter.user.sendPacketSilently(
                    new WrapperPlayServerEntityMetadata(waiter.entityId, Collections.singletonList(nameEntry)));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().error(
                    String.format("Error while sending translated hologram line to entity %d", waiter.entityId), e);
        }
    }

    /**
     * The custom name is the only chat component an armor stand carries, so it
     * is recognised by shape instead of a metadata index that shifts between
     * protocol versions. Old clients receive it as a json string inside the
     * optional, and the oldest as a bare string.
     */
    public static CustomName findCustomName(List<EntityData<?>> metadata) {
        for (EntityData<?> entry : metadata) {
            Object value = entry.getValue();

            if (value instanceof Optional && ((Optional<?>) value).isPresent()) {
                Object inner = ((Optional<?>) value).get();

                if (inner instanceof Component) {
                    String legacy = LegacyComponentSerializer.legacySection().serialize((Component) inner);
                    return new CustomName(entry, NameShape.COMPONENT, legacy);
                }

                if (inner instanceof String) {
                    try {
                        Component component = GsonComponentSerializer.gson().deserialize((String) inner);
                        String legacy = LegacyComponentSerializer.legacySection().serialize(component);
                        return new CustomName(entry, NameShape.JSON, legacy);
                    } catch (Exception e) {
                        // Not a component after all, leave the entry alone
                        return null;
                    }
                }
            }

            if (value instanceof String && !((String) value).isEmpty() && entry.getIndex() == 2) {
                // Pre 1.13 wire format, the name is a plain string on index 2
                return new CustomName(entry, NameShape.LEGACY_STRING, (String) value);
            }
        }
        return null;
    }

    /** Encodes legacy text back into the wire shape it was found in. */
    public static Object encodeName(NameShape shape, String legacy) {
        switch (shape) {
            case COMPONENT:
                return Optional.of(LegacyComponentSerializer.legacySection().deserialize(legacy));
            case JSON:
                return Optional.of(GsonComponentSerializer.gson()
                        .serialize(LegacyComponentSerializer.legacySection().deserialize(legacy)));
            case LEGACY_STRING:
            default:
                return legacy;
        }
    }

    public static boolean isInvisible(List<EntityData<?>> metadata) {
        for (EntityData<?> entry : metadata) {
            if (entry.getIndex() == 0 && entry.getValue() instanceof Byte) {
                return (((Byte) entry.getValue()) & ENTITY_FLAG_INVISIBLE) != 0;
            }
        }
        return false;
    }

    public static boolean hasTranslatableText(String legacy) {
        return LineBlock.hasTranslatableText(legacy);
    }

    public static String cacheKey(String language, String legacy) {
        return language + '\0' + legacy;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setValue(EntityData<?> entry, Object value) {
        ((EntityData) entry).setValue(value);
    }
}
