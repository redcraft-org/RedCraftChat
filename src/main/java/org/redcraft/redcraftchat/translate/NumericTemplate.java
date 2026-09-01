package org.redcraft.redcraftchat.translate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a message with numbers in it into a template that can be cached.
 *
 * The reason is a live one: CMI writes elytra speed to the action bar and
 * updates it every few ticks. "Speed: 12.3" and "Speed: 12.4" are different
 * strings, so every tick missed the provider cache and bought a fresh
 * translation of a sentence that had already been translated hundreds of
 * times. The words never change; only the number does.
 *
 * Replacing each number with a placeholder collapses all of those into one
 * cache entry, so a counter costs a single translation no matter how fast it
 * ticks. The same applies to timers, coordinates, balances and scoreboards.
 *
 * The placeholders are named rather than positional, and are shaped like the
 * %placeholder% convention the rest of the plugin already uses, so the
 * tokenizer protects them for providers that tokenize and the prompt already
 * tells the ones that do not to repeat them back unchanged. Named means a
 * translation that reorders them still puts every value back where it belongs,
 * which positional substitution would get wrong in any language that moves the
 * number relative to the noun.
 */
public class NumericTemplate {

    /**
     * A run of digits, keeping any decimal or thousands separators inside one
     * placeholder. Splitting "12.3" into two numbers would leave a bare "."
     * for the translator to move around.
     */
    // The lookbehind keeps legacy colour codes whole: the 6 in §6 is not a
    // number, and templating it shipped §%number_a% to the provider and
    // fragmented the cache by colour. A digit AFTER a code still counts:
    // in §612, §6 is the code and 12 is real text.
    private static final Pattern NUMBER = Pattern.compile("(?<!§)\\d+(?:[.,:]\\d+)*");

    /**
     * Letters only, so the placeholder matches the existing %([a-z_]*)%
     * pattern the tokenizer looks for.
     */
    private static final String[] NAMES = { "a", "b", "c", "d", "e", "f", "g", "h" };

    private final String template;
    private final Map<String, String> values;
    private final boolean templated;

    private NumericTemplate(String template, Map<String, String> values, boolean templated) {
        this.template = template;
        this.values = values;
        this.templated = templated;
    }

    /**
     * Builds the template, or a pass-through when there is nothing to gain.
     *
     * More numbers than there are names means something unusual, like a dump
     * of coordinates, and those are not worth the risk of a translation losing
     * one placeholder among a dozen.
     */
    public static NumericTemplate of(String text) {
        if (text == null || text.isEmpty()) {
            return passThrough(text);
        }

        Matcher matcher = NUMBER.matcher(text);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }

        if (found.isEmpty() || found.size() > NAMES.length) {
            return passThrough(text);
        }

        Map<String, String> values = new LinkedHashMap<>();
        StringBuilder out = new StringBuilder();
        matcher.reset();
        int index = 0;
        int last = 0;
        while (matcher.find()) {
            String name = "%number_" + NAMES[index++] + "%";
            values.put(name, matcher.group());
            out.append(text, last, matcher.start()).append(name);
            last = matcher.end();
        }
        out.append(text.substring(last));

        return new NumericTemplate(out.toString(), values, true);
    }

    private static NumericTemplate passThrough(String text) {
        return new NumericTemplate(text, new LinkedHashMap<>(), false);
    }

    /** Whether any number was lifted out. */
    public boolean isTemplated() {
        return templated;
    }

    /** What the provider should be asked to translate. */
    public String template() {
        return template;
    }

    /**
     * Puts the numbers back into a translation of the template.
     *
     * A placeholder that did not survive means the values can no longer all be
     * placed, and emitting a message with a stray %number_b% in it is worse
     * than emitting the original. The caller is told by getting its own text
     * back, which also keeps the bad translation from being displayed.
     */
    public String restore(String translated) {
        if (!templated) {
            return translated;
        }
        if (translated == null) {
            return null;
        }

        String out = translated;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!out.contains(entry.getKey())) {
                return null;
            }
            out = out.replace(entry.getKey(), entry.getValue());
        }
        return out;
    }

    /**
     * Whether the template still says something once the numbers are gone.
     *
     * An action bar that is only a number and its unit has nothing to
     * translate, so asking is pure cost.
     */
    public boolean hasWordsLeft() {
        String stripped = template;
        for (String name : values.keySet()) {
            stripped = stripped.replace(name, " ");
        }
        for (int i = 0; i < stripped.length(); i++) {
            if (Character.isLetter(stripped.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
