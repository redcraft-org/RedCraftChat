package org.redcraft.redcraftchat.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Shared plumbing for command completion.
 *
 * Two audiences, and they are served by different mechanisms.
 *
 * Java clients ask the server on every keystroke, so suggest() can return
 * whatever it likes and the answer can depend on live state. Velocity does
 * NOT prefix-filter what suggest() returns, so filtering here is real
 * behaviour rather than politeness.
 *
 * Bedrock clients never ask. Geyser builds their autocomplete once, from the
 * declared command tree, and has no round trip for suggestions at all. Only
 * literal nodes survive that translation, as enum values. Anything a Bedrock
 * player can usefully be offered has to be a literal in the hints, which is
 * why the two layers exist and why neither replaces the other.
 */
public final class Suggestions {

    private Suggestions() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * The word the player is currently typing.
     *
     * Velocity splits arguments without dropping empties, so a trailing space
     * arrives as a final "" element. That empty string is the signal that a
     * new word has started and everything should be offered, rather than the
     * previous word being completed a second time.
     */
    public static String currentWord(String[] args) {
        return args.length == 0 ? "" : args[args.length - 1];
    }

    /** How many words are complete, so a caller knows which slot it is filling. */
    public static int wordIndex(String[] args) {
        return args.length == 0 ? 0 : args.length - 1;
    }

    /** Candidates matching what has been typed so far, case-insensitively. */
    public static List<String> matching(Collection<String> candidates, String prefix) {
        List<String> out = new ArrayList<>();
        if (candidates == null) {
            return out;
        }
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (needle.isEmpty() || candidate.toLowerCase(Locale.ROOT).startsWith(needle)) {
                if (!out.contains(candidate)) {
                    out.add(candidate);
                }
            }
        }
        return out;
    }
}
