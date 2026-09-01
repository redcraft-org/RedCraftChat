package org.redcraft.redcraftchat.translate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Lets a message with clickable parts be translated without losing a click.
 *
 * The serializer the chat pipeline runs on flattens a component tree to a
 * string, and clicks, hovers and insertions do not survive that. Interactive
 * messages have therefore always been forwarded untranslated: a French
 * player got the menu, in English, rather than a menu whose buttons had
 * quietly died.
 *
 * The fix is the NumericTemplate idea applied to subtrees. Every subtree
 * carrying an event is lifted out of the tree before serialization and its
 * visible text is left inline between paired markers, so the provider
 * translates the sentence and the button labels together, with full context:
 *
 *   §6Click %click_a%here%end_a% to teleport
 *
 * The events themselves never travel. They stay here, keyed by marker name,
 * and are reattached to the translated label when the translation comes
 * back. A provider cannot corrupt a click, because it never sees one.
 *
 * The marker halves are shaped like the placeholders both provider styles
 * already protect: each independently matches the tokenizer's %([a-z_]*)%
 * pattern, and the Claude prompt names the pair rule outright. A slash form
 * like %/click_a% was considered and rejected, because the slash breaks the
 * tokenizer match and the dangling percent can eat a real placeholder.
 *
 * Everything fails closed, the NumericTemplate contract: any validation
 * failure returns null and the caller sends the original component
 * untouched, which is exactly what happened to these messages before this
 * class existed. A player never sees a marker and never loses an event.
 */
public class ComponentTemplate {

    /** Marker names, and through them the cap on lifts per message. */
    private static final char[] NAMES = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' };

    private static final Pattern MARKER = Pattern.compile("%(click|end)_([a-h])%");

    /** Anything marker-shaped already in the text disables templating. */
    private static final Pattern COLLISION = Pattern.compile("%(?:click|end)_[a-z]%");

    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    /**
     * A grown label is refused: the model swallowed neighbours into the
     * pair, and a button that silently widened its clickable region is worse
     * than one keeping its original words.
     */
    private static final int LABEL_GROWTH_FACTOR = 3;
    private static final int LABEL_GROWTH_MARGIN = 20;

    /** One lifted subtree: what came out, and how it may go back. */
    private static class Lift {
        final char name;
        /** The subtree exactly as the server built it. */
        final Component subtree;
        /** The fully resolved style at the lift point, events included. */
        final Style effectiveStyle;
        /** The visible text that went between the markers. */
        final String label;
        /**
         * Whether a translated label may replace the original words. Only a
         * childless TextComponent is simple enough: a label made of several
         * styled runs still lends the sentence its context inline, but
         * restore reuses the original subtree rather than guessing how the
         * translation distributes across the runs.
         */
        final boolean rebuildable;

        Lift(char name, Component subtree, Style effectiveStyle, String label, boolean rebuildable) {
            this.name = name;
            this.subtree = subtree;
            this.effectiveStyle = effectiveStyle;
            this.label = label;
            this.rebuildable = rebuildable;
        }
    }

    private final Component original;
    private final String originalLegacy;
    private final String skeleton;
    private final List<Lift> lifts;
    private final boolean templated;
    private final boolean interactive;

    private ComponentTemplate(Component original, String originalLegacy, String skeleton,
            List<Lift> lifts, boolean templated, boolean interactive) {
        this.original = original;
        this.originalLegacy = originalLegacy;
        this.skeleton = skeleton;
        this.lifts = lifts;
        this.templated = templated;
        this.interactive = interactive;
    }

    /**
     * Builds the template, or a pass-through when templating is not safe.
     *
     * A pass-through with {@link #hadInteractiveContent()} true means the
     * message carries events but templating refused it (too many lifts, or
     * marker-shaped text already present): the caller forwards it untouched,
     * today's behaviour. False means the message is plain and the ordinary
     * path applies.
     */
    public static ComponentTemplate of(Component message) {
        if (!(message instanceof TextComponent)) {
            return passThrough(message, false);
        }

        String flattened = serializer.serialize(message);
        if (COLLISION.matcher(flattened).find()) {
            // Refuse rather than rename: marker-shaped user text is either an
            // accident, which is rare, or somebody probing, which is a reason
            // to decline instead of negotiate
            boolean interactive = countLiftPoints(message) > 0;
            return passThrough(message, interactive);
        }

        List<Lift> lifts = new ArrayList<>();
        Component skeletonTree;
        try {
            skeletonTree = lift(message, Style.empty(), lifts);
        } catch (TooManyLifts e) {
            return passThrough(message, true);
        }

        if (lifts.isEmpty()) {
            return passThrough(message, false);
        }

        String skeleton = serializer.serialize(skeletonTree);
        return new ComponentTemplate(message, flattened, skeleton, lifts, true, true);
    }

    private static ComponentTemplate passThrough(Component message, boolean interactive) {
        return new ComponentTemplate(message, null, null, new ArrayList<>(), false, interactive);
    }

    public boolean isTemplated() {
        return templated;
    }

    /** Whether the message carried any event, templated or refused. */
    public boolean hadInteractiveContent() {
        return interactive;
    }

    /** What the provider should be asked to translate. */
    public String skeleton() {
        return skeleton;
    }

    /** The whole original as legacy text, for the hover that shows it. */
    public String originalLegacy() {
        return originalLegacy;
    }

    /**
     * Whether there is anything to translate at all. Letters inside a pair
     * count: a message that is nothing but a button still has words, which
     * makes pure-button lines translatable for the first time.
     */
    public boolean hasWordsLeft() {
        if (!templated) {
            return false;
        }
        String stripped = MARKER.matcher(skeleton).replaceAll(" ");
        stripped = stripped.replaceAll("§.", "");
        for (int i = 0; i < stripped.length(); i++) {
            if (Character.isLetter(stripped.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds the message from a translation of the skeleton.
     *
     * Null means the translation cannot be trusted to carry the events, and
     * the caller sends the original untouched. Per-pair problems degrade
     * more softly: an emptied or grown label keeps its original words while
     * the sentence translation survives.
     *
     * @param hoverForSegments attached to the translated text runs only,
     * never to a lifted subtree, so a button keeps its own hover or none
     */
    public Component restore(String translated, Component hoverForSegments) {
        if (!templated || translated == null) {
            return null;
        }

        // One pass validates the marker discipline: every pair present
        // exactly once, each open strictly before its close, no nesting and
        // no interleaving. Anything less and the events cannot be placed
        // with confidence, so nothing is.
        List<int[]> tokens = new ArrayList<>(); // start, end, nameIndex, isOpen
        Matcher matcher = MARKER.matcher(translated);
        while (matcher.find()) {
            boolean open = matcher.group(1).equals("click");
            tokens.add(new int[] { matcher.start(), matcher.end(),
                    matcher.group(2).charAt(0) - 'a', open ? 1 : 0 });
        }

        Set<Character> expected = new HashSet<>();
        for (Lift lift : lifts) {
            expected.add(lift.name);
        }

        Set<Character> closed = new HashSet<>();
        int openIndex = -1;
        for (int[] token : tokens) {
            char name = (char) ('a' + token[2]);
            if (!expected.contains(name)) {
                return null; // a marker nobody issued
            }
            if (token[3] == 1) {
                if (openIndex != -1 || closed.contains(name)) {
                    return null; // nested, interleaved, or reopened
                }
                openIndex = token[2];
            } else {
                if (openIndex != token[2]) {
                    return null; // close without its own open
                }
                closed.add(name);
                openIndex = -1;
            }
        }
        if (openIndex != -1 || !closed.equals(expected)) {
            return null; // half a pair, or a pair lost entirely
        }

        // Walk the string, alternating plain segments and pairs
        List<Component> children = new ArrayList<>();
        int cursor = 0;
        for (int i = 0; i < tokens.size(); i += 2) {
            int[] open = tokens.get(i);
            int[] close = tokens.get(i + 1);

            appendSegment(children, translated, cursor, open[0], hoverForSegments);

            Lift lift = liftByName((char) ('a' + open[2]));
            String label = translated.substring(open[1], close[0]);
            children.add(rebuildLift(lift, label));

            cursor = close[1];
        }
        appendSegment(children, translated, cursor, translated.length(), hoverForSegments);

        if (children.isEmpty()) {
            return null;
        }
        // The root carries nothing, so lifted styles owe it nothing
        return Component.text().append(children).build();
    }

    private void appendSegment(List<Component> children, String translated, int from, int to,
            Component hoverForSegments) {
        if (from >= to) {
            return;
        }
        // Legacy colour is stateful across the whole string, and the split
        // severed that state: a segment after a gold label would otherwise
        // fall back to white
        String segment = activeLegacyState(translated, from) + translated.substring(from, to);
        Component built = serializer.deserialize(segment);
        if (hoverForSegments != null) {
            built = built.hoverEvent(HoverEvent.showText(hoverForSegments));
        }
        children.add(built);
    }

    private Component rebuildLift(Lift lift, String translatedLabel) {
        String visible = translatedLabel.replaceAll("§.", "");

        boolean usable = lift.rebuildable
                && hasLetters(visible)
                && visible.length() <= lift.label.length() * LABEL_GROWTH_FACTOR + LABEL_GROWTH_MARGIN;

        if (!usable) {
            // The sentence translation survives; the button keeps its own
            // words, which beats guessing
            return Component.text().style(lift.effectiveStyle).append(lift.subtree).build();
        }

        // Click, hover and insertion are style properties in Adventure, so
        // the resolved style wrapper carries the events and the colours
        // through one mechanism
        return Component.text().style(lift.effectiveStyle)
                .append(serializer.deserialize(translatedLabel)).build();
    }

    private Lift liftByName(char name) {
        for (Lift lift : lifts) {
            if (lift.name == name) {
                return lift;
            }
        }
        throw new IllegalStateException("validated marker without a lift: " + name);
    }

    /** Signals the lift cap without threading a counter through the walk. */
    private static class TooManyLifts extends RuntimeException {
        TooManyLifts() {
            super(null, null, false, false);
        }
    }

    /**
     * The copy-with-replacement walk. Outermost wins: a lifted node's whole
     * subtree travels as one unit, so nested events stay together.
     */
    private static Component lift(Component node, Style parentEffective, List<Lift> lifts) {
        Style effective = node.style().merge(parentEffective, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);

        if (carriesEvent(node)) {
            if (lifts.size() >= NAMES.length) {
                throw new TooManyLifts();
            }
            char name = NAMES[lifts.size()];

            boolean rebuildable = node instanceof TextComponent && node.children().isEmpty();
            String label = rebuildable
                    ? ((TextComponent) node).content()
                    : serializer.serialize(node);

            lifts.add(new Lift(name, node, effective, label, rebuildable));
            return Component.text("%click_" + name + "%" + label + "%end_" + name + "%");
        }

        if (node.children().isEmpty()) {
            return node;
        }

        List<Component> children = new ArrayList<>();
        for (Component child : node.children()) {
            children.add(lift(child, effective, lifts));
        }
        return node.children(children);
    }

    private static boolean carriesEvent(Component node) {
        return node.clickEvent() != null || node.hoverEvent() != null || node.insertion() != null;
    }

    private static int countLiftPoints(Component node) {
        int count = carriesEvent(node) ? 1 : 0;
        if (count == 1) {
            return 1;
        }
        for (Component child : node.children()) {
            count += countLiftPoints(child);
            if (count > 0) {
                return count;
            }
        }
        return count;
    }

    private static boolean hasLetters(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The legacy colour and decoration state active at an index, as the
     * prefix that reproduces it. A colour code or reset clears decorations,
     * the way the client's own renderer treats them.
     */
    public static String activeLegacyState(String text, int upTo) {
        String color = null;
        List<Character> decorations = new ArrayList<>();
        for (int i = 0; i + 1 < upTo && i < text.length() - 1; i++) {
            if (text.charAt(i) != '§') {
                continue;
            }
            char code = Character.toLowerCase(text.charAt(i + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = "§" + code;
                decorations.clear();
            } else if (code == 'r') {
                color = null;
                decorations.clear();
            } else if (code >= 'k' && code <= 'o') {
                if (!decorations.contains(code)) {
                    decorations.add(code);
                }
            }
        }
        StringBuilder prefix = new StringBuilder();
        if (color != null) {
            prefix.append(color);
        }
        for (char decoration : decorations) {
            prefix.append('§').append(decoration);
        }
        return prefix.toString();
    }
}
