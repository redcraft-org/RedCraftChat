import junit.framework.*;

import java.util.Arrays;
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
