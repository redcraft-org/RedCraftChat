import junit.framework.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.redcraft.redcraftchat.RedCraftChat;

import com.velocitypowered.api.plugin.Plugin;

/**
 * Velocity wants the plugin version as a compile time constant, so it cannot
 * come from the pom and the two drift silently. They did: a release tagged
 * 0.1.4 shipped a jar identifying itself as 0.1.3, and the plugins updater,
 * which names files by the version inside the jar, kept serving what looked
 * like the previous release.
 */
public class PluginVersionTest extends TestCase {

    public void testThePluginAnnotationMatchesThePom() throws Exception {
        String pom = new String(Files.readAllBytes(Paths.get("pom.xml")), StandardCharsets.UTF_8);

        // The first version element is the project's own, the plugin pins
        // further down all come later in the file
        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(pom);
        assertTrue("No version in pom.xml", matcher.find());

        Plugin annotation = RedCraftChat.class.getAnnotation(Plugin.class);

        assertEquals(matcher.group(1), annotation.version());
    }
}
