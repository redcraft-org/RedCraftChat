import junit.framework.*;

import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.detection.DetectionManager;

public class DetectionManagerTest extends TestCase {
	protected void setUp() {
        Config.translationSupportedLanguages.add("fr");
        Config.translationSupportedLanguages.add("en");
    }

    public void testFrench() {
        String expectedDetection = "fr";
        String actualDetection = DetectionManager.getLanguage("Bonjour mon nom est lululombard");
        assertEquals(expectedDetection, actualDetection);
    }

    public void testEnglish() {
        String expectedDetection = "en";
        String actualDetection = DetectionManager.getLanguage("Hello my name is lululombard");
        assertEquals(expectedDetection, actualDetection);
    }
}
