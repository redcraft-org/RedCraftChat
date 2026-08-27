import junit.framework.*;

import java.util.HashSet;

import org.redcraft.redcraftchat.locales.UiStrings;

/**
 * A string missing from ALL still works but is translated on the first
 * player who sees it instead of ahead of time, which is exactly the
 * half-translated-menu bug the warmer exists to prevent.
 */
public class UiStringsSelectorTest extends TestCase {

    public void test_every_selector_string_is_warmed() {
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_TITLE));
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_FIRST_JOIN_HELP));
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_DONE));
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_JOIN_PROMPT));
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_KEEP_CURRENT));
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_APPEARED));
        assertTrue(UiStrings.ALL.contains(UiStrings.SELECTOR_CONFIRMED));
    }

    public void test_no_duplicates_pool_translation_budget() {
        assertEquals(UiStrings.ALL.size(), new HashSet<>(UiStrings.ALL).size());
    }
}
