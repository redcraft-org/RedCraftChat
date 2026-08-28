import junit.framework.*;

import org.redcraft.redcraftchat.displaykit.LanguageSelectorSession;

/**
 * Setting a main language also files it under "understood", so trying one
 * primary and then picking another used to leave the first behind as a
 * secondary the player never asked for, pre-ticked on the second step.
 *
 * The flow undoes only what it did itself: a language the player already
 * understood before the panel opened is never taken away from them.
 */
public class SelectorPrimarySwitchTest extends TestCase {

    public void testTryingAPrimaryThenSwitchingDropsTheFirst() {
        // Picks Français: nothing understood yet, so the flow added it
        String auto = LanguageSelectorSession.rememberAutoAdded(null, "fr-FR", false);
        assertEquals("fr-FR", auto);

        // Switches to English: Français goes back out
        assertEquals("fr-FR", LanguageSelectorSession.languageToDropOnPrimaryChange(auto, "en-US"));
        assertEquals("en-US", LanguageSelectorSession.rememberAutoAdded(auto, "en-US", false));
    }

    public void testRepickingTheSamePrimaryDropsNothingAndStaysTracked() {
        String auto = LanguageSelectorSession.rememberAutoAdded(null, "fr-FR", false);
        assertNull(LanguageSelectorSession.languageToDropOnPrimaryChange(auto, "fr-FR"));
        // Still known to be self-added, so a later switch still cleans it up
        assertEquals("fr-FR", LanguageSelectorSession.rememberAutoAdded(auto, "fr-FR", true));
        assertEquals("fr-FR", LanguageSelectorSession.languageToDropOnPrimaryChange(
                LanguageSelectorSession.rememberAutoAdded(auto, "fr-FR", true), "de-DE"));
    }

    public void testALanguageTheyAlreadyUnderstoodIsNeverTakenAway() {
        // German was already in their list before the panel opened
        String auto = LanguageSelectorSession.rememberAutoAdded(null, "de-DE", true);
        assertNull(auto);
        assertNull(LanguageSelectorSession.languageToDropOnPrimaryChange(auto, "en-US"));
    }

    public void testNothingIsDroppedBeforeAnyPrimaryIsPicked() {
        assertNull(LanguageSelectorSession.languageToDropOnPrimaryChange(null, "en-US"));
    }

    public void testSwitchingAwayThenOntoAnUnderstoodLanguageStopsTracking() {
        String auto = LanguageSelectorSession.rememberAutoAdded(null, "fr-FR", false);
        // Italian was ticked on step two, so picking it as primary is not
        // something the flow invented
        assertEquals("fr-FR", LanguageSelectorSession.languageToDropOnPrimaryChange(auto, "it-IT"));
        assertNull(LanguageSelectorSession.rememberAutoAdded(auto, "it-IT", true));
    }
}
