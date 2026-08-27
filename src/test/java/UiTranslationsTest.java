import java.util.Arrays;
import java.util.List;

import junit.framework.*;

import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.locales.UiTranslations;

/**
 * The interface's own words are a fixed set, so they are translated by hand
 * rather than by a machine seeing one line with nothing around it. A gap in
 * the table is not a failure, it just falls back to the runtime translator,
 * but a gap in the strings that label buttons is exactly what the hand
 * translations exist to prevent.
 */
public class UiTranslationsTest extends TestCase {

    private static final List<String> BUTTONS = Arrays.asList(
            UiStrings.SELECTOR_SUBMIT,
            UiStrings.CHANGE_MAIN_LANGUAGE,
            UiStrings.SELECTOR_CONTINUE,
            UiStrings.SELECTOR_NEXT,
            UiStrings.SELECTOR_BACK,
            UiStrings.SELECTOR_CLOSE,
            UiStrings.SELECTOR_DONE);

    public void testEverySupportedLanguageCoversTheButtons() {
        for (String language : UiTranslations.languages()) {
            for (String button : BUTTONS) {
                assertNotNull("no " + language + " translation for '" + button + "'",
                        UiTranslations.lookup(button, language));
            }
        }
    }

    public void testTheRegionIsIgnored() {
        // Preferences carry "fr-FR"; these words do not differ by region
        assertEquals(UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, "fr"),
                UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, "fr-FR"));
        assertEquals(UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, "fr"),
                UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, "FR_fr"));
    }

    public void testAnUnknownLanguageFallsThrough() {
        // Null means "translate it the usual way", never an empty label
        assertNull(UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, "ja-JP"));
        assertNull(UiTranslations.lookup("something a player said", "fr-FR"));
        assertNull(UiTranslations.lookup(null, "fr-FR"));
        assertNull(UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, null));
    }

    public void testTranslationsAreNotJustTheEnglishBack() {
        for (String language : UiTranslations.languages()) {
            String submit = UiTranslations.lookup(UiStrings.SELECTOR_SUBMIT, language);
            assertFalse(language + " left Submit untranslated", UiStrings.SELECTOR_SUBMIT.equals(submit));
        }
    }

    public void testTheHelpLinesAreCoveredToo() {
        // These are the ones a machine mangled: a pronoun with no antecedent
        for (String language : UiTranslations.languages()) {
            assertNotNull(UiTranslations.lookup(UiStrings.SELECTOR_PRIMARY_HELP, language));
            assertNotNull(UiTranslations.lookup(UiStrings.SELECTOR_OTHERS_HELP, language));
        }
    }
}
