import java.util.List;

import junit.framework.*;

import org.redcraft.redcraftchat.displaykit.LanguageSelectorSession;

/**
 * An action slot the page does not fill still occupies its row and paints
 * nothing, which shows up in game as a hole in the panel, and one action too
 * many splits the menu across a page break. Both steps want one row per
 * language plus the button that ends the step, so this one number is what
 * keeps the panel whole.
 */
public class SelectorGeometryTest extends TestCase {

    public void testRowsAreExactlyTheLanguages() {
        // The buttons that end a step live below the menu now, so the list
        // holds nothing but languages
        assertEquals(6, LanguageSelectorSession.menuRowsFor(6));
        assertEquals(2, LanguageSelectorSession.menuRowsFor(2));
    }

    public void testRowsNeverCollapseOrRunAwayWithTheSurface() {
        assertEquals(1, LanguageSelectorSession.menuRowsFor(0));
        assertEquals(1, LanguageSelectorSession.menuRowsFor(1));
        // Beyond the clamp the menu paginates rather than growing a panel
        // taller than the player can comfortably read
        assertEquals(9, LanguageSelectorSession.menuRowsFor(20));
    }

    public void testHeightCoversHelpMenuAndBothButtons() {
        int menuRows = LanguageSelectorSession.menuRowsFor(6);
        // The menu paints a title and one row per language. Its three
        // navigation slots collapse to nothing, but each still costs a gap.
        int menuChildren = 1 + menuRows + 3;
        int menuHeight = (1 + menuRows) * 20 + (menuChildren - 1) * 2;
        // help, menu, step button, close, and the three gaps between them
        int expected = LanguageSelectorSession.helpHeightPx() + menuHeight + 2 * 20 + 3 * 2;
        assertEquals(expected, LanguageSelectorSession.surfaceHeightPx(menuRows));
    }

    public void testHeightGrowsOneRowAtATime() {
        assertEquals(22, LanguageSelectorSession.surfaceHeightPx(7) - LanguageSelectorSession.surfaceHeightPx(6));
    }

    public void testWrapKeepsWholeWordsWithinTheWidth() {
        List<String> lines = LanguageSelectorSession.wrap(
                "Ticked languages reach you as written, never translated", 214, 2);
        assertTrue("expected the text to need more than one line", lines.size() > 1);
        assertTrue(lines.size() <= 2);
        // No word is broken across lines
        assertEquals("Ticked languages reach you as written, never translated",
                String.join(" ", lines).replace("...", ""));
    }

    public void testWrapNeverExceedsTheLineBudget() {
        // A translation far longer than the panel must degrade, not overflow
        StringBuilder giant = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            giant.append("langue ");
        }
        List<String> lines = LanguageSelectorSession.wrap(giant.toString().trim(), 214, 2);
        assertEquals(2, lines.size());
    }

    public void testWrapSaysSoWhenItDropsWords() {
        StringBuilder giant = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            giant.append("langue ");
        }
        List<String> lines = LanguageSelectorSession.wrap(giant.toString().trim(), 214, 2);
        // Silently cutting the sentence short would read as the whole message
        assertTrue("truncation must be visible", lines.get(1).endsWith("..."));
    }

    public void testWrapDoesNotClaimTruncationWhenNothingWasDropped() {
        List<String> lines = LanguageSelectorSession.wrap(
                "Ticked languages reach you as written, never translated", 214, 2);
        for (String line : lines) {
            assertFalse(line.endsWith("..."));
        }
    }

    public void testWrapKeepsAWordWiderThanThePanelInsideIt() {
        List<String> lines = LanguageSelectorSession.wrap(
                "Sprachauswahlbestaetigungseinstellungsuebersicht", 60, 2);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).endsWith("..."));
    }

    public void testWrapHandlesTextShorterThanOneLine() {
        List<String> lines = LanguageSelectorSession.wrap("Next", 214, 2);
        assertEquals(1, lines.size());
        assertEquals("Next", lines.get(0));
    }
}
