import junit.framework.*;

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
}
