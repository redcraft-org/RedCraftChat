package org.redcraft.redcraftchat.translate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.helpers.LegacyText;

/**
 * Text that reaches a player as several separate lines but reads as one.
 *
 * A hologram is a column of armor stands, and a plugin printing a menu sends
 * one chat packet per line, so both arrive as fragments. Translated one by one
 * those fragments lose the sentence they belonged to, and a tail line like
 * "server!" cannot even be language detected on its own. They are therefore
 * joined into a block for the provider and taken apart again afterwards.
 *
 * Splitting the answer back up is what makes this safe: a provider that
 * reflowed the block into a different number of lines cannot be mapped back
 * onto the entities or packets it came from, so the caller is told to fall
 * back to translating each line by itself rather than shown text in the wrong
 * places.
 */
public final class LineBlock {

    private LineBlock() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String join(List<String> lines) {
        return String.join("\n", lines);
    }

    /**
     * A line with no letters, arrows and dividers mostly, would only confuse
     * the language detector. The colour codes are stripped first, they carry
     * letters of their own.
     */
    public static boolean hasTranslatableText(String legacy) {
        return LegacyText.stripColor(legacy).codePoints().anyMatch(Character::isLetter);
    }

    /**
     * Maps each source line to its line of the block translation, null unless
     * the structure held: one output line per input line, with text wherever
     * the source had text.
     */
    public static Map<String, String> align(List<String> sourceLines, String translatedBlock) {
        String[] translatedLines = translatedBlock.split("\n", -1);
        if (translatedLines.length != sourceLines.size()) {
            return null;
        }

        Map<String, String> aligned = new LinkedHashMap<>();
        for (int i = 0; i < translatedLines.length; i++) {
            if (hasTranslatableText(sourceLines.get(i)) && !hasTranslatableText(translatedLines[i])) {
                // A line dropped to nothing is a reshaped block, not a
                // translation of that line
                return null;
            }
            aligned.put(sourceLines.get(i), translatedLines[i]);
        }
        return aligned;
    }
}
