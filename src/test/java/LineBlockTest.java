import junit.framework.*;

import java.util.Arrays;
import java.util.Map;

import org.redcraft.redcraftchat.translate.LineBlock;

/**
 * Text that reaches a player as separate lines is translated as one block, so
 * the model reads a whole sentence rather than a fragment. Taking the answer
 * apart again is the risky half: a line handed back to the wrong hologram
 * entity or the wrong chat packet is worse than an untranslated one, so
 * anything that does not map cleanly must be refused rather than guessed at.
 */
public class LineBlockTest extends TestCase {

    public void testEachLineGetsItsSlice() {
        Map<String, String> aligned = LineBlock.align(
                Arrays.asList("Welcome to the", "Creative Build", "server!"),
                "Добро пожаловать на\nCreative Build\nсервер!");

        assertNotNull(aligned);
        assertEquals("Добро пожаловать на", aligned.get("Welcome to the"));
        assertEquals("Creative Build", aligned.get("Creative Build"));
        assertEquals("сервер!", aligned.get("server!"));
    }

    public void testAMergedBlockDoesNotAlign() {
        // Three lines rewritten as one sentence kept the meaning but lost the
        // structure, there is no way back to three packets
        assertNull(LineBlock.align(
                Arrays.asList("Welcome to the", "Creative Build", "server!"),
                "Добро пожаловать на Creative Build сервер!"));
    }

    public void testASplitLineDoesNotAlign() {
        // More lines out than in, the extra one belongs to no packet
        assertNull(LineBlock.align(
                Arrays.asList("Welcome to the Creative Build server!"),
                "Добро пожаловать на\nCreative Build сервер!"));
    }

    public void testALineDroppedToNothingDoesNotAlign() {
        // Right line count, but a line with text came back empty: that is a
        // reshaped block, not a translation of that line
        assertNull(LineBlock.align(
                Arrays.asList("Welcome to the", "server!"),
                "Добро пожаловать на сервер!\n"));
    }

    public void testADecorativeLineMayComeBackUnchanged() {
        // A divider has nothing to translate, so it is not evidence of a
        // reshaped block when it comes back as it went in
        Map<String, String> aligned = LineBlock.align(
                Arrays.asList("§7-----", "Welcome!", "§7-----"),
                "§7-----\nBienvenue !\n§7-----");

        assertNotNull(aligned);
        assertEquals("Bienvenue !", aligned.get("Welcome!"));
        assertEquals("§7-----", aligned.get("§7-----"));
    }

    public void testJoiningIsWhatTheProviderSees() {
        assertEquals("Welcome to the\nserver!", LineBlock.join(Arrays.asList("Welcome to the", "server!")));
    }

    public void testColourCodesAreNotWords() {
        // §e carries an e, §a an a, stripping has to happen before looking
        // for letters or every divider looks translatable
        assertFalse(LineBlock.hasTranslatableText("§e▲ ▲ ▲"));
        assertFalse(LineBlock.hasTranslatableText("§a§l-------"));
        assertTrue(LineBlock.hasTranslatableText("§7You need to be a §bMember§7 to join"));
        assertTrue(LineBlock.hasTranslatableText("§7Réservé aux §bMembres"));
    }
}
