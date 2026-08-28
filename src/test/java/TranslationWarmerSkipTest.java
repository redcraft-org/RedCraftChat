import junit.framework.*;

import org.redcraft.redcraftchat.locales.TranslationWarmer;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.locales.UiTranslations;

/**
 * Warming a string that already has a hand-written translation buys a cache
 * entry nothing ever reads: localizeUiForPlayer answers from UiTranslations
 * and returns before it reaches the cache. Every string translated by hand
 * should therefore make the warmup cheaper, not more expensive.
 */
public class TranslationWarmerSkipTest extends TestCase {

    public void testHandWrittenStringsAreNotPaidFor() {
        assertFalse(TranslationWarmer.needsWarming(UiStrings.SELECTOR_SUBMIT, "fr"));
        assertFalse(TranslationWarmer.needsWarming(UiStrings.SELECTOR_PRIMARY_TITLE, "ru"));
    }

    public void testStringsWithoutOneStillGoToTheProvider() {
        // The fallback is the point: no hand-written entry means the machine
        // still handles it, warmed as before
        assertTrue(TranslationWarmer.needsWarming(UiStrings.DISCORD_RUN_COMMAND, "fr"));
        assertTrue(TranslationWarmer.needsWarming(UiStrings.LEGEND, "de"));
    }

    public void testALanguageWithNoTableIsWarmedInFull() {
        // Adding a locale and nothing else has to keep working
        for (String uiString : UiStrings.ALL) {
            assertTrue("a language with no hand-written table must still be warmed",
                    TranslationWarmer.needsWarming(uiString, "ja"));
        }
    }

    public void testTheSavingMatchesWhatIsTranslatedByHand() {
        int skipped = 0;
        for (String language : UiTranslations.languages()) {
            for (String uiString : UiStrings.ALL) {
                if (!TranslationWarmer.needsWarming(uiString, language)) {
                    skipped++;
                }
            }
        }
        // Every hand-written entry in every language is one call not made
        assertTrue("expected the hand-written strings to be skipped, got " + skipped, skipped > 0);
        int expected = 0;
        for (String language : UiTranslations.languages()) {
            for (String uiString : UiStrings.ALL) {
                if (UiTranslations.lookup(uiString, language) != null) {
                    expected++;
                }
            }
        }
        assertEquals(expected, skipped);
    }
}
