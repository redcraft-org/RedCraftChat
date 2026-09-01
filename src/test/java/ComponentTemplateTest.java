import junit.framework.*;

import org.redcraft.redcraftchat.translate.ComponentTemplate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * The whole contract in pins. This class sits in front of every chat message
 * once its flag is on, so the failure rows matter more than the happy path:
 * a player must never see a marker and never lose a click.
 */
public class ComponentTemplateTest extends TestCase {

    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    private Component clickable(String label, String command) {
        return Component.text(label).clickEvent(ClickEvent.runCommand(command));
    }

    /** The menu line every test reasons about. */
    private Component menuLine() {
        return Component.text()
                .content("Click ")
                .color(NamedTextColor.GOLD)
                .append(clickable("here", "/tp museum"))
                .append(Component.text(" to teleport"))
                .build();
    }

    /** Finds the first node in the tree carrying a click, however deep. */
    private Component findClickable(Component node) {
        if (node.clickEvent() != null || node.style().clickEvent() != null) {
            return node;
        }
        for (Component child : node.children()) {
            Component found = findClickable(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private String flat(Component node) {
        return PlainTextComponentSerializer.plainText().serialize(node);
    }

    public void test_the_case_this_was_written_for() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        assertTrue(template.isTemplated());
        assertTrue(template.hasWordsLeft());
        // The label rides inline between its pair, so the provider sees the
        // sentence whole
        // No code re-emitted after the pair: the marker child inherits the
        // root's gold, so the colour state never changes across it
        assertEquals("§6Click %click_a%here%end_a% to teleport",
                template.skeleton().replace("§r", ""));

        Component restored = template.restore(
                "§6Cliquez %click_a%ici%end_a%§6 pour vous téléporter", null);
        assertNotNull(restored);
        assertEquals("Cliquez ici pour vous téléporter", flat(restored));

        Component button = findClickable(restored);
        assertNotNull("the click must survive translation", button);
        assertEquals("/tp museum", clickCommand(button));
        assertEquals("ici", flat(button));
    }

    @SuppressWarnings("rawtypes")
    private String clickCommand(Component button) {
        ClickEvent event = button.clickEvent() != null ? button.clickEvent()
                : button.style().clickEvent();
        // Adventure 5 moved the command into a typed payload
        return ((ClickEvent.Payload.Text) event.payload()).value();
    }

    public void test_restore_survives_word_reorder() {
        // A language that fronts the button: the segments land around it and
        // the command still fires from the right words
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        Component restored = template.restore("%click_a%hier%end_a% klicken zum Teleportieren", null);
        assertNotNull(restored);
        assertEquals("hier klicken zum Teleportieren", flat(restored));
        assertEquals("/tp museum", clickCommand(findClickable(restored)));
    }

    public void test_a_lost_pair_forwards_nothing() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        assertNull(template.restore("Cliquez pour vous téléporter", null));
    }

    public void test_a_half_open_pair_is_refused() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        assertNull(template.restore("Cliquez %click_a%ici pour vous téléporter", null));
    }

    public void test_a_foreign_marker_is_refused() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        assertNull(template.restore("Cliquez %click_b%ici%end_b%", null));
    }

    public void test_a_reopened_pair_is_refused() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        assertNull(template.restore(
                "%click_a%ici%end_a% et %click_a%là%end_a%", null));
    }

    public void test_interleaved_pairs_are_refused() {
        Component two = Component.text()
                .append(clickable("yes", "/accept"))
                .append(Component.text(" or "))
                .append(clickable("no", "/deny"))
                .build();
        ComponentTemplate template = ComponentTemplate.of(two);
        assertNull(template.restore(
                "%click_a%oui %click_b%non%end_a%%end_b%", null));
    }

    public void test_swapped_complete_pairs_still_bind_their_own_events() {
        Component two = Component.text()
                .append(clickable("yes", "/accept"))
                .append(Component.text(" or "))
                .append(clickable("no", "/deny"))
                .build();
        ComponentTemplate template = ComponentTemplate.of(two);
        // The translation fronts the refusal; each label keeps its own command
        Component restored = template.restore(
                "%click_b%non%end_b% ou %click_a%oui%end_a%", null);
        assertNotNull(restored);
        assertEquals("non ou oui", flat(restored));

        // The first clickable in reading order is now the /deny one
        Component first = findClickable(restored);
        assertEquals("/deny", clickCommand(first));
        assertEquals("non", flat(first));
    }

    public void test_two_identical_labels_stay_distinct() {
        Component two = Component.text()
                .append(clickable("here", "/warp a"))
                .append(Component.text(" and "))
                .append(clickable("here", "/warp b"))
                .build();
        ComponentTemplate template = ComponentTemplate.of(two);
        Component restored = template.restore(
                "%click_a%ici%end_a% et %click_b%ici%end_b%", null);
        assertNotNull(restored);
        assertEquals("/warp a", clickCommand(findClickable(restored)));
    }

    public void test_an_emptied_label_keeps_its_original_words() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        Component restored = template.restore(
                "§6Cliquez %click_a%%end_a%§6 pour vous téléporter", null);
        assertNotNull("the sentence translation must survive", restored);
        // The button fell back to its own words rather than vanishing
        Component button = findClickable(restored);
        assertEquals("here", flat(button));
        assertEquals("/tp museum", clickCommand(button));
    }

    public void test_a_grown_label_keeps_its_original_words() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        String swollen = "ici pour vous téléporter vers le musée magnifique immédiatement";
        Component restored = template.restore(
                "§6Cliquez %click_a%" + swollen + "%end_a%", null);
        assertNotNull(restored);
        assertEquals("here", flat(findClickable(restored)));
    }

    public void test_a_multi_run_label_lends_context_but_keeps_its_words() {
        Component fancy = Component.text()
                .append(Component.text("Open "))
                .append(Component.text()
                        .append(Component.text("the ", NamedTextColor.RED))
                        .append(Component.text("menu", NamedTextColor.AQUA))
                        .build().clickEvent(ClickEvent.runCommand("/menu")))
                .build();
        ComponentTemplate template = ComponentTemplate.of(fancy);
        assertTrue(template.isTemplated());

        Component restored = template.restore(
                "Ouvrez %click_a%le menu%end_a%", null);
        assertNotNull(restored);
        // The translated label is discarded for a styled multi-run button;
        // the original runs and their colours come back verbatim
        Component button = findClickable(restored);
        assertEquals("the menu", flat(button));
        assertEquals("/menu", clickCommand(button));
    }

    public void test_literal_marker_text_disables_templating() {
        Component probing = Component.text()
                .content("try %click_a% this ")
                .append(clickable("button", "/x"))
                .build();
        ComponentTemplate template = ComponentTemplate.of(probing);
        assertFalse(template.isTemplated());
        // Interactive but refused: the caller forwards it untouched
        assertTrue(template.hadInteractiveContent());
    }

    public void test_more_than_eight_buttons_bails_out() {
        TextComponent.Builder many = Component.text();
        for (int i = 0; i < 9; i++) {
            many.append(clickable("b" + i, "/cmd" + i));
        }
        ComponentTemplate template = ComponentTemplate.of(many.build());
        assertFalse(template.isTemplated());
        assertTrue(template.hadInteractiveContent());
    }

    public void test_nested_events_lift_at_the_outermost_node() {
        Component nested = Component.text()
                .content("See ")
                .append(Component.text("details")
                        .hoverEvent(HoverEvent.showText(Component.text("hover")))
                        .clickEvent(ClickEvent.runCommand("/details")))
                .build();
        ComponentTemplate template = ComponentTemplate.of(nested);
        assertTrue(template.isTemplated());

        Component restored = template.restore("Voir %click_a%détails%end_a%", null);
        Component button = findClickable(restored);
        assertEquals("/details", clickCommand(button));
        // The hover travelled with it, byte-identical intent
        assertNotNull(button.hoverEvent() != null ? button.hoverEvent()
                : button.style().hoverEvent());
    }

    public void test_a_hover_only_run_is_lifted_and_kept() {
        // Today a hover-only run is not even caught by isInteractive and its
        // hover is silently destroyed; lifting fixes that in passing
        Component hoverful = Component.text()
                .content("The item ")
                .append(Component.text("[Sword]")
                        .hoverEvent(HoverEvent.showText(Component.text("Sharpness V"))))
                .build();
        ComponentTemplate template = ComponentTemplate.of(hoverful);
        assertTrue(template.isTemplated());

        Component restored = template.restore("L'objet %click_a%[Épée]%end_a%", null);
        assertNotNull(restored);
        Component item = findHoverable(restored);
        assertNotNull("the hover must survive", item);
        assertEquals("[Épée]", flat(item));
    }

    private Component findHoverable(Component node) {
        if (node.hoverEvent() != null || node.style().hoverEvent() != null) {
            return node;
        }
        for (Component child : node.children()) {
            Component found = findHoverable(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void test_the_original_hover_covers_segments_only() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        Component original = Component.text("the original");
        Component restored = template.restore(
                "§6Cliquez %click_a%ici%end_a%§6 pour y aller", original);
        assertNotNull(restored);

        // Segments carry the original-text hover; the click-only button
        // must not sprout one
        Component button = findClickable(restored);
        assertNull("a click-only button must not gain a hover",
                button.style().hoverEvent());

        boolean segmentHasHover = false;
        for (Component child : restored.children()) {
            if (findClickable(child) == null && child.hoverEvent() != null) {
                segmentHasHover = true;
            }
        }
        assertTrue("translated text carries the original on hover", segmentHasHover);
    }

    public void test_effective_style_survives_the_rebuild() {
        // Green parent, unstyled clickable child: re-parented under the
        // style-empty rebuilt root, the label must still resolve green
        Component greenMenu = Component.text()
                .content("Pick ")
                .color(NamedTextColor.GREEN)
                .append(clickable("this", "/pick"))
                .build();
        ComponentTemplate template = ComponentTemplate.of(greenMenu);
        Component restored = template.restore("Choisissez %click_a%ceci%end_a%", null);

        Component button = findClickable(restored);
        assertEquals(NamedTextColor.GREEN, button.style().color());
    }

    public void test_colour_state_crosses_a_pair() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        // The translation carries no colour code after the pair; the
        // trailing words must still be gold, not white
        Component restored = template.restore(
                "§6Cliquez %click_a%ici%end_a% maintenant", null);
        assertNotNull(restored);

        String rebuilt = legacy.serialize(restored);
        int tail = rebuilt.indexOf("maintenant");
        assertTrue(tail > 0);
        assertEquals("the trailing segment keeps the active colour",
                "§6", ComponentTemplate.activeLegacyState(rebuilt, tail));
    }

    public void test_a_pure_button_line_is_translatable() {
        // A message that is nothing but a button used to be untouchable;
        // with the label inline it finally has words
        Component button = Component.text()
                .append(clickable("Teleport to spawn", "/spawn"))
                .build();
        ComponentTemplate template = ComponentTemplate.of(button);
        assertTrue(template.isTemplated());
        assertTrue(template.hasWordsLeft());
    }

    public void test_a_plain_message_is_not_this_classs_business() {
        ComponentTemplate template = ComponentTemplate.of(Component.text("just words"));
        assertFalse(template.isTemplated());
        assertFalse(template.hadInteractiveContent());
    }

    public void test_marker_halves_match_the_tokenizer_pattern() {
        // Both halves must independently match %([a-z_]*)% or the tokenizing
        // providers would leave them exposed. This is also why %/click_a%
        // was rejected: the slash breaks the match and the dangling percent
        // can pair with a real placeholder's.
        java.util.regex.Pattern protection = java.util.regex.Pattern.compile("%([a-z_]*)%");
        assertTrue(protection.matcher("%click_a%").matches());
        assertTrue(protection.matcher("%end_a%").matches());
        assertFalse(protection.matcher("%/click_a%").matches());
    }

    public void test_null_and_empty_do_not_throw() {
        ComponentTemplate template = ComponentTemplate.of(menuLine());
        assertNull(template.restore(null, null));
        assertNull(template.restore("", null));
        assertFalse(ComponentTemplate.of(Component.text("")).isTemplated());
    }
}
