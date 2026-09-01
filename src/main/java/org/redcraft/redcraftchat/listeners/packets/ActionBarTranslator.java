package org.redcraft.redcraftchat.listeners.packets;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.translate.LineBlock;
import org.redcraft.redcraftchat.translate.NumericTemplate;
import org.redcraft.redcraftchat.translate.TranslationManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Translates action bars, both ways they arrive.
 *
 * The action bar has two wire forms and they are different packets: the
 * dedicated SET_ACTION_BAR_TEXT that Adventure's sendActionBar produces, and
 * SYSTEM_CHAT_MESSAGE with the overlay flag, which is how CMI writes its. The
 * chat interceptor only ever saw the second, which is why the elytra speed
 * was translated while the museum's "Use /musehub to quit" loop never was.
 *
 * This is not a sibling of SystemChatInterceptor's buffer, on purpose. That
 * design cancels, holds messages for a grouping window, and re-emits from a
 * scheduler thread, which is what multi-line menus need and exactly what a
 * surface repainting twenty times a second cannot afford: two frames
 * perpetually in flight and re-emitted against the pipeline is visible
 * jitter, for text that self-corrects one repeat later anyway. So this
 * follows HologramTranslator instead: rewrite in place on the netty thread
 * when the cache already has the answer, pass the original through and warm
 * the cache in the background when it does not. The first repeat after a
 * miss is translated; nothing is ever held or reordered.
 *
 * Counters make the cache work. NumericTemplate collapses every frame of
 * "Time: 12.34 | Fails: 0" onto one template, the template is what gets
 * translated and cached, and each frame injects its own numbers into the
 * cached translation. A twenty-hertz HUD costs one provider call, ever.
 */
public class ActionBarTranslator extends PacketListenerAbstract {

    /**
     * New, uncached templates a player may produce inside the window before
     * the channel goes quiet. Counting misses rather than text changes is
     * what makes a cyclic animation safe: it stops missing once the cache
     * has seen a full cycle, where a change counter would trip forever.
     */
    public static final int NEW_TEMPLATE_LIMIT = 8;
    static final long NEW_TEMPLATE_WINDOW_MILLIS = 10_000;
    public static final long QUIET_MILLIS = 60_000;

    /** Provider failures back off rather than retrying twenty times a second. */
    static final long FAILURE_RETRY_MILLIS = 60_000;

    /** Same admission cap as the hologram cache, for the same reason. */
    static final int MAX_CACHED_TEMPLATES = 10_000;

    static TranslationManager translationManager = new TranslationManager(Config.upstreamTranslationProvider);

    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    /**
     * language + '\0' + template -> result. Identity results are cached too,
     * so a bar that needs no translation is never detected twice. This is the
     * only map the netty thread reads.
     */
    private static final Map<String, HologramTranslator.CachedTranslation> translations = new ConcurrentHashMap<>();

    /** Templates whose last provider call failed, and when. */
    private static final Map<String, Long> failures = new ConcurrentHashMap<>();

    /** Templates with a background translation already running. */
    private static final Map<String, Boolean> pending = new ConcurrentHashMap<>();

    private static final Map<UUID, HologramTranslator.PlayerLanguages> languageByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Object> resolveLocks = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> resolving = new ConcurrentHashMap<>();

    private static final Map<UUID, ChurnGuard> churnByPlayer = new ConcurrentHashMap<>();

    public ActionBarTranslator() {
        super(PacketListenerPriority.NORMAL);
    }

    /**
     * Preference-change hook, called from updatePlayerPreferences next to the
     * hologram one. No re-push needed here: the next frame repaints itself.
     */
    public static void onPreferencesUpdated(UUID playerId) {
        if (playerId != null) {
            languageByPlayer.remove(playerId);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Component text;
        boolean overlaySystemChat = false;

        if (event.getPacketType() == PacketType.Play.Server.ACTION_BAR) {
            text = new WrapperPlayServerActionBar(event).getActionBarText();
        } else if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            // Only the overlay form belongs to this listener. Ordinary system
            // chat stays with the interceptor and its grouping buffer, and
            // the two conditions are disjoint so the split cannot double-send.
            WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
            if (!wrapper.isOverlay()) {
                return;
            }
            overlaySystemChat = true;
            text = wrapper.getMessage();
        } else {
            return;
        }

        Object receiver = event.getPlayer();
        if (!(receiver instanceof Player)) {
            event.markForReEncode(false);
            return;
        }
        Player player = (Player) receiver;


        // A TranslatableComponent is vanilla text the client localizes
        // itself; serializing it would bake the fallback string in
        if (!(text instanceof TextComponent)) {
            event.markForReEncode(false);
            return;
        }

        String legacy = serializer.serialize(text);
        if (!LineBlock.hasTranslatableText(legacy)) {
            event.markForReEncode(false);
            return;
        }

        NumericTemplate frame = NumericTemplate.of(legacy);
        if (frame.isTemplated() && !frame.hasWordsLeft()) {
            // A clock is not a sentence
            event.markForReEncode(false);
            return;
        }
        String template = frame.isTemplated() ? frame.template() : legacy;

        HologramTranslator.PlayerLanguages languages = languageByPlayer.get(player.getUniqueId());
        if (languages == null) {
            // This frame goes through as-is; the resolve runs off-thread and
            // the next repeat finds the answer waiting
            scheduleLanguageResolve(player);
            event.markForReEncode(false);
            return;
        }

        HologramTranslator.CachedTranslation cached = translations.get(cacheKey(languages.target, template));
        if (cached == null) {
            scheduleTranslation(player, legacy, template, languages.target);
            event.markForReEncode(false);
            return;
        }

        String rewritten = rewriteFromCache(frame, template, cached, languages);
        if (rewritten == null) {
            event.markForReEncode(false);
            return;
        }

        Component replacement = serializer.deserialize(rewritten);
        if (overlaySystemChat) {
            WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
            wrapper.setMessage(replacement);
        } else {
            new WrapperPlayServerActionBar(event).setActionBarText(replacement);
        }
        event.markForReEncode(true);
    }

    /**
     * The pure heart of the fast path, package-visible so the tests can pin
     * it without packets: this frame's numbers go into the cached translated
     * template. Null means leave the original alone.
     */
    public static String rewriteFromCache(NumericTemplate frame, String template,
            HologramTranslator.CachedTranslation cached, HologramTranslator.PlayerLanguages languages) {
        if (cached.translated.equals(template)) {
            // Identity result: detection punted or it was already the
            // target language
            return null;
        }
        if (cached.source != null && languages.spoken.contains(cached.source)) {
            // The reader speaks what the server wrote, same rule as chat
            // and holograms
            return null;
        }
        if (!frame.isTemplated()) {
            return cached.translated;
        }
        // Restore checks that this frame's own placeholders survived; it
        // cannot know about extras a provider invented, and a stray
        // %number_b% on screen is exactly the thing this feature must never
        // show. The output is checked whole.
        String restored = frame.restore(cached.translated);
        if (restored == null || restored.contains("%number_")) {
            return null;
        }
        return restored;
    }

    private void scheduleLanguageResolve(Player player) {
        UUID playerId = player.getUniqueId();
        if (resolving.putIfAbsent(playerId, Boolean.TRUE) != null) {
            return;
        }
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                // Same lock discipline as the hologram path and for the same
                // reason: the resolve blocks on the preference backend
                Object lock = resolveLocks.computeIfAbsent(playerId, id -> new Object());
                synchronized (lock) {
                    if (languageByPlayer.get(playerId) == null) {
                        HologramTranslator.PlayerLanguages languages = HologramTranslator.resolveLanguages(player);
                        if (languages != null) {
                            languageByPlayer.put(playerId, languages);
                        }
                    }
                }
            } finally {
                resolving.remove(playerId);
            }
        }).schedule();
    }

    private void scheduleTranslation(Player player, String rawLegacy, String template, String targetLanguage) {
        long now = System.currentTimeMillis();

        Long failedAt = failures.get(cacheKey(targetLanguage, template));
        if (failedAt != null && now - failedAt < FAILURE_RETRY_MILLIS) {
            return;
        }

        // The churn guard charges the player for scheduling new provider
        // work, not for repainting: cache hits stay free however fast they
        // come, so one marquee cannot silence a session, only pause new
        // spending on it
        ChurnGuard guard = churnByPlayer.computeIfAbsent(player.getUniqueId(), id -> new ChurnGuard());
        if (guard.recordMissAndCheckQuiet(now)) {
            return;
        }

        String key = cacheKey(targetLanguage, template);
        if (pending.putIfAbsent(key, Boolean.TRUE) != null) {
            // Twenty players staring at the museum loop is one provider call
            return;
        }

        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try {
                // Detection reads the raw frame, numbers and all: "Time: 12"
                // detects, "%number_a%" does not. Translation reads the
                // template so the cached result is reusable across frames;
                // its placeholders carry no digits, so the inner
                // NumericTemplate passes them straight to the provider, which
                // is already told to copy %placeholders% verbatim.
                String sourceLanguage = DetectionManager.getLanguage(rawLegacy);
                if (sourceLanguage == null) {
                    // Detection has nothing to say about twenty characters of
                    // imperative, and caching that as an identity buried the
                    // museum bar in English forever. Upstream text on this
                    // network is written in one language, so assume it: with
                    // an LLM provider that is self-correcting, since it sees
                    // the actual words regardless of the claimed source.
                    sourceLanguage = Config.upstreamFallbackSourceLanguage;
                    if (sourceLanguage == null || sourceLanguage.isEmpty()) {
                        sourceLanguage = null;
                    }
                }

                String translated;
                String source;
                if (sourceLanguage == null) {
                    source = null;
                    translated = template;
                } else if (sourceLanguage.equalsIgnoreCase(targetLanguage)) {
                    source = sourceLanguage.toLowerCase();
                    translated = template;
                } else {
                    source = sourceLanguage.toLowerCase();
                    translated = translationManager.translate(template, sourceLanguage, targetLanguage);
                }

                // A translation that lost a placeholder can never be
                // injected into, so it is stored as an identity result
                // rather than retried per frame: providers are deterministic
                // enough that a retry would just buy the same answer again
                if (!placeholdersSurvived(template, translated)) {
                    translated = template;
                }

                if (translations.size() < MAX_CACHED_TEMPLATES) {
                    translations.put(key, new HologramTranslator.CachedTranslation(source, translated));
                }
                failures.remove(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failures.put(key, System.currentTimeMillis());
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().error(
                        String.format("Error while translating action bar [-> %s] %s", targetLanguage, template), e);
                failures.put(key, System.currentTimeMillis());
            } finally {
                pending.remove(key);
            }
        }).schedule();
    }

    /** Every %number_x% the template had must still be in the translation. */
    public static boolean placeholdersSurvived(String template, String translated) {
        if (translated == null) {
            return false;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("%number_[a-z]%").matcher(template);
        while (matcher.find()) {
            if (!translated.contains(matcher.group())) {
                return false;
            }
        }
        return true;
    }

    public static String cacheKey(String language, String template) {
        return language + '\0' + template;
    }

    /**
     * The per-player sliding window over cache misses. Takes its clock as an
     * argument so the tests do not have to wait sixty real seconds.
     */
    public static class ChurnGuard {
        private final Deque<Long> misses = new ArrayDeque<>();
        private long quietUntil;

        /** Records one scheduled miss; true means stay quiet, spend nothing. */
        public synchronized boolean recordMissAndCheckQuiet(long now) {
            if (now < quietUntil) {
                return true;
            }
            misses.addLast(now);
            while (!misses.isEmpty() && now - misses.peekFirst() > NEW_TEMPLATE_WINDOW_MILLIS) {
                misses.removeFirst();
            }
            if (misses.size() > NEW_TEMPLATE_LIMIT) {
                quietUntil = now + QUIET_MILLIS;
                misses.clear();
                return true;
            }
            return false;
        }
    }

    /** Session teardown, called on disconnect from the display name listener's sibling hook. */
    public static void onDisconnect(UUID playerId) {
        if (playerId != null) {
            languageByPlayer.remove(playerId);
            resolveLocks.remove(playerId);
            churnByPlayer.remove(playerId);
        }
    }
}
