import junit.framework.*;

import org.redcraft.redcraftchat.displaykit.LanguageSelectorSession;

/**
 * An action slot the page does not fill still occupies its row and paints
 * nothing, which shows up in game as a hole in the panel, and one action too
 * many splits the menu across a page break. Both steps want exactly one row
 * per language, so these two numbers are what keep the panel whole.
 */
public class SelectorGeometryTest extends TestCase {

    public void testRowsFollowTheLanguageCount() {
        // Six supported locales: six to pick from on step one, five to tick
        // plus Done on step two, so both steps fill the same six rows
        assertEquals(6, LanguageSelectorSession.menuRowsFor(6));
        assertEquals(2, LanguageSelectorSession.menuRowsFor(2));
    }

    public void testRowsNeverCollapseOrRunAwayWithTheSurface() {
        assertEquals(1, LanguageSelectorSession.menuRowsFor(0));
        assertEquals(1, LanguageSelectorSession.menuRowsFor(1));
        // Beyond the clamp the menu paginates rather than growing a panel
        // taller than the player can look at
        assertEquals(8, LanguageSelectorSession.menuRowsFor(20));
    }

    public void testHeightIsTitlePlusRowsPlusNavigationWithGaps() {
        // 1 title + 6 actions + 3 navigation = 10 rows of 20px, 9 gaps of 2px
        assertEquals(218, LanguageSelectorSession.surfaceHeightPx(6));
        assertEquals(20 * 5 + 2 * 4, LanguageSelectorSession.surfaceHeightPx(1));
    }

    public void testHeightGrowsOneRowAtATime() {
        int six = LanguageSelectorSession.surfaceHeightPx(6);
        int seven = LanguageSelectorSession.surfaceHeightPx(7);
        assertEquals(22, seven - six);
    }
}
