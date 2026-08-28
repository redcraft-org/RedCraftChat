import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.*;

import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.locales.UiTranslations;

/**
 * Guards the shape of every interface string against the two ways a value
 * spliced into one goes missing.
 *
 * Both of these have already shipped once. "Mail sent to " lost its trailing
 * space in translation and printed as "Mail envoye a.lululombard", because a
 * translator has no reason to keep whitespace at the end of a sentence.
 */
public class UiStringSeparatorTest extends TestCase {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-z_]+)%");

    /**
     * A string that leans on a leading or trailing space is relying on
     * whitespace surviving a round trip through a translator, which it does
     * not. Anything joined to a name or a number wants a placeholder instead,
     * which also lets a language put it mid-sentence.
     */
    public void test_no_string_depends_on_an_edge_space() {
        List<String> offenders = new ArrayList<>();
        for (String message : UiStrings.ALL) {
            if (!message.equals(message.trim())) {
                offenders.add("\"" + message + "\"");
            }
        }
        assertEquals("interface strings padded with spaces: " + offenders,
                0, offenders.size());
    }

    /**
     * A hand translation that drops a placeholder cannot have the value put
     * back into it, so the player is told half a sentence: "Mail sent to"
     * with no name, or an unread count with no count.
     */
    public void test_every_hand_translation_keeps_its_placeholders() {
        List<String> offenders = new ArrayList<>();
        for (String message : UiStrings.ALL) {
            List<String> wanted = placeholders(message);
            if (wanted.isEmpty()) {
                continue;
            }
            for (String language : UiTranslations.languages()) {
                String translated = UiTranslations.lookup(message, language);
                if (translated == null) {
                    continue;
                }
                for (String name : wanted) {
                    if (!translated.contains(name)) {
                        offenders.add(language + " lost " + name + " in \"" + translated + "\"");
                    }
                }
            }
        }
        assertEquals("translations missing a placeholder: " + offenders, 0, offenders.size());
    }

    /** And nothing invents a placeholder the caller will never substitute. */
    public void test_no_translation_invents_a_placeholder() {
        List<String> offenders = new ArrayList<>();
        for (String message : UiStrings.ALL) {
            List<String> wanted = placeholders(message);
            for (String language : UiTranslations.languages()) {
                String translated = UiTranslations.lookup(message, language);
                if (translated == null) {
                    continue;
                }
                for (String found : placeholders(translated)) {
                    if (!wanted.contains(found)) {
                        offenders.add(language + " added " + found + " to \"" + translated + "\"");
                    }
                }
            }
        }
        assertEquals("translations with a placeholder nobody fills: " + offenders,
                0, offenders.size());
    }

    private List<String> placeholders(String message) {
        List<String> found = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(message);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }
}
