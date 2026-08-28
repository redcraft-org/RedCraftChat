import junit.framework.*;

import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;

public class LocaleEndonymTest extends TestCase {

    private SupportedLocale locale(String code, String name) {
        return new SupportedLocale(code, name);
    }

    public void test_languages_name_themselves() {
        assertEquals("Français", LocaleManager.getEndonym(locale("fr-FR", "French")));
        assertEquals("English", LocaleManager.getEndonym(locale("en-US", "English")));
        assertEquals("Deutsch", LocaleManager.getEndonym(locale("de-DE", "German")));
    }

    public void test_underscore_codes_work_too() {
        assertEquals("Français", LocaleManager.getEndonym(locale("fr_FR", "French")));
    }

    public void test_the_first_letter_is_capitalized() {
        // The JVM says "français"; players read a list, it should be a name
        String endonym = LocaleManager.getEndonym(locale("fr-FR", "French"));
        assertTrue(Character.isUpperCase(endonym.charAt(0)));
    }

    public void test_unknown_tags_fall_back_to_the_stored_name() {
        assertEquals("Klingon", LocaleManager.getEndonym(locale("zz-ZZ", "Klingon")));
    }
}
