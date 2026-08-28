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
     * the line count held.
     *
     * The count is the whole contract, and deliberately so. Where a line ends
     * is an accident of the display, while languages disagree about word
     * order: English puts "server" after the name and French puts it before,
     * so "Welcome to the / Creative Build / server!" has to become "Bienvenue
     * sur le / serveur Créatif Build" and the words must cross a line break to
     * get there. Demanding that each line translate to itself corners the
     * engine into leaving a line in the source language, which is what it did.
     *
     * So an individual line may come back empty, having given its words to the
     * line above. Only a block that lost its text altogether is refused, since
     * that is a reply that went wrong rather than one that was laid out
     * differently.
     */
    public static Map<String, String> align(List<String> sourceLines, String translatedBlock) {
        String[] translatedLines = translatedBlock.split("\n", -1);
        if (translatedLines.length != sourceLines.size()) {
            return null;
        }

        boolean sourceHadText = false;
        boolean translationHasText = false;
        Map<String, String> aligned = new LinkedHashMap<>();

        for (int i = 0; i < translatedLines.length; i++) {
            sourceHadText |= hasTranslatableText(sourceLines.get(i));
            translationHasText |= hasTranslatableText(translatedLines[i]);
            aligned.put(sourceLines.get(i), translatedLines[i]);
        }

        return sourceHadText && !translationHasText ? null : aligned;
    }
}
