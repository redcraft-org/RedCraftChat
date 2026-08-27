import junit.framework.*;

import org.redcraft.redcraftchat.dialog.NativeDialogSelector;

/**
 * Minecraft validates a dialog input key against the resource-path charset.
 * A key outside it does not degrade: the client fails to decode the whole
 * show_dialog packet and drops the connection, so this is the difference
 * between a working panel and every player being kicked.
 */
public class DialogInputKeyTest extends TestCase {

    private static final String ALLOWED = "[a-z0-9_.-]+";

    public void testLocaleCodesBecomeLegalKeys() {
        // Locale codes are mixed case and hyphenated, neither of which survives
        assertEquals("understood_fr_fr", NativeDialogSelector.understoodKey("fr-FR"));
        assertEquals("understood_en_us", NativeDialogSelector.understoodKey("en-US"));
        assertEquals("understood_ru_ru", NativeDialogSelector.understoodKey("ru-RU"));
    }

    public void testEverySupportedShapeMatchesTheCharset() {
        String[] codes = {"fr-FR", "en-US", "es-ES", "de-DE", "it-IT", "ru-RU", "pt-BR", "zh-CN"};
        for (String code : codes) {
            String key = NativeDialogSelector.understoodKey(code);
            assertTrue("key '" + key + "' would be rejected by the client", key.matches(ALLOWED));
        }
    }

    public void testTheKeyIsStableSoTheAnswerCanBeReadBack() {
        // The dialog writes it and the click listener reads it, so the two
        // must agree for every code without a shared lookup table
        assertEquals(NativeDialogSelector.understoodKey("de-DE"),
                NativeDialogSelector.understoodKey("de-DE"));
        assertFalse(NativeDialogSelector.understoodKey("de-DE")
                .equals(NativeDialogSelector.understoodKey("en-US")));
    }
}
