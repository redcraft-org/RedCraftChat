import java.util.Arrays;
import java.util.List;

import junit.framework.*;

import org.redcraft.redcraftchat.commands.Suggestions;

/**
 * Velocity does not prefix-filter what suggest() returns, so this filtering
 * is the behaviour a player sees, not a nicety.
 */
public class SuggestionsTest extends TestCase {

    private static final List<String> VERBS = Arrays.asList("list", "listall", "read", "send");

    public void testATrailingSpaceStartsANewWord() {
        // Velocity keeps the empty element, and that is how we know the player
        // finished a word rather than being partway through one
        assertEquals("", Suggestions.currentWord(new String[]{"send", ""}));
        assertEquals(1, Suggestions.wordIndex(new String[]{"send", ""}));
        assertEquals("se", Suggestions.currentWord(new String[]{"se"}));
        assertEquals(0, Suggestions.wordIndex(new String[]{"se"}));
        assertEquals("", Suggestions.currentWord(new String[]{}));
    }

    public void testAnEmptyPrefixOffersEverything() {
        assertEquals(4, Suggestions.matching(VERBS, "").size());
        assertEquals(4, Suggestions.matching(VERBS, null).size());
    }

    public void testFilteringIsByPrefixAndCaseInsensitive() {
        assertEquals(Arrays.asList("list", "listall"), Suggestions.matching(VERBS, "lis"));
        assertEquals(Arrays.asList("list", "listall"), Suggestions.matching(VERBS, "LIS"));
        assertEquals(Arrays.asList("read"), Suggestions.matching(VERBS, "r"));
        assertTrue(Suggestions.matching(VERBS, "zz").isEmpty());
    }

    public void testItIsAPrefixMatchNotASubstringOne() {
        // "all" appears inside listall but should not offer it
        assertTrue(Suggestions.matching(VERBS, "all").isEmpty());
    }

    public void testDuplicatesAndBlanksAreDropped() {
        List<String> messy = Arrays.asList("read", "read", "", null, "reply");
        List<String> out = Suggestions.matching(messy, "re");
        assertEquals(Arrays.asList("read", "reply"), out);
    }

    public void testNullCandidatesAreSurvivable() {
        // getSupportedLocales can return null on a provider failure
        assertTrue(Suggestions.matching(null, "x").isEmpty());
    }
}
