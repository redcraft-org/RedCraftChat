import junit.framework.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.redcraft.redcraftchat.listeners.packets.ActionBarTranslator;
import org.redcraft.redcraftchat.listeners.packets.HologramTranslator;
import org.redcraft.redcraftchat.translate.NumericTemplate;

/**
 * The fast path's contract, without packets: what a cached template does to a
 * frame, and when the churn guard stops the spending.
 */
public class ActionBarTranslatorTest extends TestCase {

    private HologramTranslator.PlayerLanguages french() {
        return new HologramTranslator.PlayerLanguages("fr", new HashSet<>(Arrays.asList("fr")));
    }

    private HologramTranslator.PlayerLanguages frenchWhoSpeaksEnglish() {
        return new HologramTranslator.PlayerLanguages("fr", new HashSet<>(Arrays.asList("fr", "en")));
    }

    public void test_a_numeric_hud_is_one_template() {
        // Two frames of the parkour HUD land on the same cache key, which is
        // the whole economics of the feature
        NumericTemplate a = NumericTemplate.of("Time: 12.34 | Level: 3/9 | Fails: 0");
        NumericTemplate b = NumericTemplate.of("Time: 12.39 | Level: 3/9 | Fails: 1");
        assertEquals(a.template(), b.template());
        assertEquals(ActionBarTranslator.cacheKey("fr", a.template()),
                ActionBarTranslator.cacheKey("fr", b.template()));
    }

    public void test_a_cache_hit_carries_this_frames_numbers() {
        NumericTemplate frame = NumericTemplate.of("Time: 48.02 | Fails: 3");
        String template = frame.template();
        HologramTranslator.CachedTranslation cached = new HologramTranslator.CachedTranslation(
                "en", "Temps : %number_a% | Chutes : %number_b%");

        assertEquals("Temps : 48.02 | Chutes : 3",
                ActionBarTranslator.rewriteFromCache(frame, template, cached, french()));
    }

    public void test_a_bar_without_numbers_rewrites_whole() {
        // The museum loop: no digits, the cached text is the answer as-is
        NumericTemplate frame = NumericTemplate.of("Use /musehub to quit");
        HologramTranslator.CachedTranslation cached = new HologramTranslator.CachedTranslation(
                "en", "Utilisez /musehub pour sortir");
        assertEquals("Utilisez /musehub pour sortir",
                ActionBarTranslator.rewriteFromCache(frame, "Use /musehub to quit", cached, french()));
    }

    public void test_a_player_speaking_the_source_keeps_the_original() {
        NumericTemplate frame = NumericTemplate.of("Use /musehub to quit");
        HologramTranslator.CachedTranslation cached = new HologramTranslator.CachedTranslation(
                "en", "Utilisez /musehub pour sortir");
        assertNull(ActionBarTranslator.rewriteFromCache(frame, "Use /musehub to quit", cached,
                frenchWhoSpeaksEnglish()));
    }

    public void test_an_identity_result_changes_nothing() {
        // Detection punted, or it was already the target language: the cache
        // remembers that as the template itself, and the frame passes through
        NumericTemplate frame = NumericTemplate.of("Vitesse: 12.3");
        HologramTranslator.CachedTranslation cached = new HologramTranslator.CachedTranslation(
                "fr", frame.template());
        assertNull(ActionBarTranslator.rewriteFromCache(frame, frame.template(), cached, french()));
    }

    public void test_a_lost_placeholder_is_never_stored_as_injectable() {
        // The store-time validation: a translation missing %number_b% counts
        // as failed and the identity template is what gets cached instead
        assertTrue(ActionBarTranslator.placeholdersSurvived(
                "Time: %number_a% | Fails: %number_b%",
                "Temps : %number_a% | Chutes : %number_b%"));
        assertFalse(ActionBarTranslator.placeholdersSurvived(
                "Time: %number_a% | Fails: %number_b%",
                "Temps : %number_a% | Chutes :"));
        assertFalse(ActionBarTranslator.placeholdersSurvived(
                "Time: %number_a%", null));
    }

    public void test_template_churn_trips_the_guard() {
        ActionBarTranslator.ChurnGuard guard = new ActionBarTranslator.ChurnGuard();
        long now = 1_000_000;
        // NEW_TEMPLATE_LIMIT misses inside the window are allowed
        for (int i = 0; i < ActionBarTranslator.NEW_TEMPLATE_LIMIT; i++) {
            assertFalse("miss " + i + " should still spend", guard.recordMissAndCheckQuiet(now + i * 100));
        }
        // The one past the limit flips the channel quiet
        assertTrue(guard.recordMissAndCheckQuiet(now + 900));
        // And it stays quiet inside the quiet window
        assertTrue(guard.recordMissAndCheckQuiet(now + 900 + ActionBarTranslator.QUIET_MILLIS - 1));
        // Then recovers
        assertFalse(guard.recordMissAndCheckQuiet(now + 1000 + ActionBarTranslator.QUIET_MILLIS));
    }

    public void test_spread_out_new_bars_are_not_churn() {
        ActionBarTranslator.ChurnGuard guard = new ActionBarTranslator.ChurnGuard();
        long now = 1_000_000;
        // One new template every two seconds, forever: the window never
        // holds more than a handful, the guard never trips
        for (int i = 0; i < 100; i++) {
            assertFalse(guard.recordMissAndCheckQuiet(now + i * 2_000L));
        }
    }

    public void test_the_frames_own_restore_guards_template_drift() {
        // A frame whose numbers do not line up with the cached template's
        // placeholders yields null, and null means leave the original alone
        NumericTemplate frame = NumericTemplate.of("Fails: 3");
        HologramTranslator.CachedTranslation cached = new HologramTranslator.CachedTranslation(
                "en", "Temps : %number_a% | Chutes : %number_b%");
        // restore() finds %number_b% has no value from this frame... the
        // stricter property: a one-number frame against a two-placeholder
        // translation must not emit a stray placeholder
        assertNull("a translation carrying a foreign placeholder must not be shown",
                ActionBarTranslator.rewriteFromCache(frame, frame.template(), cached, french()));
    }
}
