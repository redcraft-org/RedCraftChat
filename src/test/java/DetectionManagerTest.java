import junit.framework.*;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.detection.DetectionManager;

public class DetectionManagerTest extends TestCase {
	protected void setUp() {
        Config.translationSupportedLanguages.add("fr");
        Config.translationSupportedLanguages.add("en");
    }

    public void testFrench() {
        String expectedDetection = "fr";
        String actualDetection = DetectionManager.getLanguage("Bonjour mon nom est Peter");
        assertEquals(expectedDetection, actualDetection);
    }

    public void testEnglish() {
        String expectedDetection = "en";
        String actualDetection = DetectionManager.getLanguage("Hello my name is Peter");
        assertEquals(expectedDetection, actualDetection);
    }

    public void testUnknown() {
        String expectedDetection = null;
        String actualDetection = DetectionManager.getLanguage("Maya nama eta Peter");
        assertEquals(expectedDetection, actualDetection);
    }
}
