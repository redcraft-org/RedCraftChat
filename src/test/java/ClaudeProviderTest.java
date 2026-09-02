import junit.framework.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.translate.providers.ClaudeProvider;
import org.redcraft.redcraftchat.translate.providers.DeeplProvider;
import org.redcraft.redcraftchat.translate.providers.ModernmtProvider;

/**
 * Claude is handed the message exactly as the server wrote it, colour codes,
 * placeholders and line breaks included, and is told in the prompt what those
 * mean. The engines that cannot be told anything keep the tokenizer.
 *
 * The trade has one sharp edge: the tokenizer used to rewrite colour codes to
 * digits, so anything looking for letters could not trip over them. Raw text
 * brings the letters in §a and §l back, and a divider must still count as
 * nothing to translate.
 */
public class ClaudeProviderTest extends TestCase {

    /**
     * The name rules read three static fields, and a test that leaves one
     * populated changes the answer for every test after it. They are replaced
     * rather than cleared: some tests assign an immutable list, and clear()
     * throws on those.
     */
    @Override
    protected void setUp() {
        Config.serverDisplayNames = new LinkedHashMap<>();
        Config.translatableServerNames = new ArrayList<>();
        Config.protectedNames = new ArrayList<>();
    }

    /**
     * Names that are not servers still have to survive the round trip. The
     * museum's worlds came back as RoyaumeCollines and Construction libre,
     * which reads fine and points at a world of neither name.
     */
    public void test_protected_names_are_listed_verbatim() {
        Config.protectedNames = new ArrayList<>(Arrays.asList("KingdomHills", "Freebuild"));

        String rules = ClaudeProvider.serverNameRules();
        assertTrue(rules.contains("KingdomHills"));
        assertTrue(rules.contains("Freebuild"));
        assertTrue(rules.contains("Copy each of those exactly"));
    }

    /*
     * Listing the names in the prompt is a request the model can lose, and it
     * did, twice, on the museum holograms. These cover the guarantee that
     * replaced it: a name the model never sees cannot come back translated.
     */
    public void test_a_protected_name_is_hidden_before_the_model_sees_it() {
        Config.protectedNames = new ArrayList<>(Arrays.asList("KingdomHills"));
        Config.translatableServerNames = new ArrayList<>();
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("Welcome to KingdomHills");
        assertEquals("Welcome to {0}", mask.masked());
        assertFalse(mask.isEmpty());
        assertEquals("Bienvenue sur KingdomHills", mask.restore("Bienvenue sur {0}"));
    }

    public void test_a_name_the_model_dropped_fails_closed() {
        // The whole point. A translation that lost the placeholder cannot have
        // its names put back, so it comes back null and the caller sends the
        // original English rather than a French name for a world that has none.
        Config.protectedNames = new ArrayList<>(Arrays.asList("KingdomHills"));
        Config.translatableServerNames = new ArrayList<>();
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("Welcome to KingdomHills");
        assertNull(mask.restore("Bienvenue sur CollinesRoyaume"));
    }

    public void test_the_longest_name_is_hidden_first() {
        // Otherwise RedCraft takes a bite out of RedCraftChat and the leftover
        // Chat gets translated on its own.
        Config.protectedNames = new ArrayList<>(Arrays.asList("RedCraft", "RedCraftChat"));
        Config.translatableServerNames = new ArrayList<>();
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("RedCraftChat on RedCraft");
        assertEquals("RedCraftChat on RedCraft", mask.restore(mask.masked()));
        assertFalse(mask.masked().contains("RedCraft"));
    }

    public void test_a_name_appearing_twice_comes_back_twice() {
        Config.protectedNames = new ArrayList<>(Arrays.asList("TopRed"));
        Config.translatableServerNames = new ArrayList<>();
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("TopRed and TopRed");
        assertEquals("{0} and {0}", mask.masked());
        assertEquals("TopRed et TopRed", mask.restore("{0} et {0}"));
    }

    public void test_text_that_already_has_a_placeholder_is_left_alone() {
        // Masking here would make our placeholder and theirs impossible to tell
        // apart on the way back, so it goes unmasked and keeps the prompt rule.
        Config.protectedNames = new ArrayList<>(Arrays.asList("KingdomHills"));
        Config.translatableServerNames = new ArrayList<>();
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("{0} joined KingdomHills");
        assertTrue(mask.isEmpty());
        assertEquals("{0} joined KingdomHills", mask.masked());
        assertEquals("anything", mask.restore("anything"));
    }

    public void test_a_name_that_is_opted_back_out_is_not_hidden() {
        Config.protectedNames = new ArrayList<>(Arrays.asList("Freebuild"));
        Config.translatableServerNames = new ArrayList<>(Arrays.asList("Freebuild"));
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("Welcome to Freebuild");
        assertTrue(mask.isEmpty());
        assertEquals("Welcome to Freebuild", mask.masked());
    }

    public void test_a_line_that_is_only_a_name_needs_no_translating() {
        // Most hologram titles. Once the name is hidden there are no words
        // left, so nothing is sent and nothing can come back wrong.
        Config.protectedNames = new ArrayList<>(Arrays.asList("KingdomHills"));
        Config.translatableServerNames = new ArrayList<>();
        Config.serverDisplayNames = new LinkedHashMap<>();

        ClaudeProvider.NameMask mask = ClaudeProvider.NameMask.of("§6KingdomHills");
        assertFalse(mask.isEmpty());
        assertFalse(ClaudeProvider.hasTranslatableContent(mask.masked()));
    }

    public void test_a_protected_name_can_still_be_opted_back_out() {
        // The escape hatch works the same for these as for server names, so a
        // word that turns out to be ordinary can be handed back
        Config.protectedNames = new ArrayList<>(Arrays.asList("Freebuild"));
        Config.translatableServerNames = new ArrayList<>(Arrays.asList("Freebuild"));

        assertEquals("", ClaudeProvider.serverNameRules());
    }

    public void testOnlyClaudeSkipsTheTokenizer() {
        assertTrue(new ClaudeProvider().translatesRawText());
        assertFalse(new DeeplProvider().translatesRawText());
        assertFalse(new ModernmtProvider().translatesRawText());
    }

    public void testColourCodesAreNotMistakenForWords() {
        assertFalse(ClaudeProvider.hasTranslatableContent("§a▲ ▲ ▲"));
        assertFalse(ClaudeProvider.hasTranslatableContent("§b§l-------------"));
        assertFalse(ClaudeProvider.hasTranslatableContent("§e§m            "));
        assertFalse(ClaudeProvider.hasTranslatableContent(""));
        assertFalse(ClaudeProvider.hasTranslatableContent(null));
    }

    public void testRealTextIsStillTranslatable() {
        assertTrue(ClaudeProvider.hasTranslatableContent("§7You need to be a §bMember"));
        assertTrue(ClaudeProvider.hasTranslatableContent("§6Réservé aux membres"));
        assertTrue(ClaudeProvider.hasTranslatableContent("Welcome to the\n§6Creative Build\nserver!"));
    }

    private static void configureServers() {
        Config.serverDisplayNames = new LinkedHashMap<String, String>();
        Config.serverDisplayNames.put("hub", "&6Hub");
        Config.serverDisplayNames.put("crea_build_plot", "&dCrea Build Plot");
        Config.serverDisplayNames.put("museum", "&eMuseum");
        Config.translatableServerNames = Arrays.asList("Museum");
    }

    public void testTheServerNamesAreSpelledOutForTheModel() {
        // "Leave server names alone" is unactionable, the model cannot know
        // which words are names: it rendered Creative Build as Construction
        // Créative next to a sign reading Créatif Build
        configureServers();
        String rules = ClaudeProvider.serverNameRules();

        assertTrue(rules.contains("Crea Build Plot"));
        assertTrue(rules.contains("Hub"));
        // The ids travel too, players type them after /server
        assertTrue(rules.contains("crea_build_plot"));
        assertTrue(rules.contains("museum"));
    }

    public void testAnOrdinaryWordIsLeftOutOfTheVerbatimList() {
        // Museum reads better as Musée, so the display name is excluded from
        // the copy exactly list and called out as translatable instead
        configureServers();
        String rules = ClaudeProvider.serverNameRules();

        String verbatim = rules.substring(0, rules.indexOf("Copy each"));
        assertFalse(verbatim.contains("Museum"));
        assertTrue(rules.contains("Museum is an ordinary word"));
        // The lowercase id stays verbatim, it is a command argument
        assertTrue(verbatim.contains("museum"));
    }

    public void testSurroundingWordsAreStillTranslated() {
        // "Available servers: crea_build_plot, hub, museum" came back wholly
        // untranslated, the line being mostly names
        configureServers();
        assertTrue(ClaudeProvider.serverNameRules().contains("still translated as usual"));
    }

    public void testNoServersConfiguredAddsNothing() {
        Config.serverDisplayNames = new LinkedHashMap<String, String>();
        Config.translatableServerNames = Arrays.asList();

        assertEquals("", ClaudeProvider.serverNameRules());
    }
}
