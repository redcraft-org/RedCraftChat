import junit.framework.*;

import org.redcraft.redcraftchat.translate.NumericTemplate;

/**
 * The templating sits in front of every translated message, so its failure
 * mode matters more than its saving: a message must never reach a player with
 * a placeholder still in it, and must never come back with the wrong number.
 */
public class NumericTemplateTest extends TestCase {

    public void test_the_case_this_was_written_for() {
        // CMI's elytra speed, which changes every few ticks. Both values have
        // to produce the same template or the cache never hits.
        NumericTemplate slow = NumericTemplate.of("Speed: 12.3 km/h");
        NumericTemplate fast = NumericTemplate.of("Speed: 48.9 km/h");

        assertEquals(slow.template(), fast.template());
        assertEquals("Speed: %number_a% km/h", slow.template());

        // and each still restores its own number
        assertEquals("Vitesse: 12.3 km/h", slow.restore("Vitesse: %number_a% km/h"));
        assertEquals("Vitesse: 48.9 km/h", fast.restore("Vitesse: %number_a% km/h"));
    }

    public void test_a_decimal_stays_one_placeholder() {
        // Splitting 12.3 would leave a bare "." for the translator to move
        NumericTemplate template = NumericTemplate.of("Speed: 12.3");
        assertEquals("Speed: %number_a%", template.template());
    }

    public void test_names_survive_reordering() {
        // German puts the time before the noun. Positional substitution would
        // put the minutes where the hours belong; named placeholders cannot.
        NumericTemplate template = NumericTemplate.of("4 hours and 25 minutes");
        assertEquals("4 hours and 25 minutes",
                template.restore("%number_a% hours and %number_b% minutes"));
        assertEquals("25 minutes after 4 hours",
                template.restore("%number_b% minutes after %number_a% hours"));
    }

    public void test_a_lost_placeholder_yields_nothing_rather_than_a_stray_token() {
        NumericTemplate template = NumericTemplate.of("Speed: 12.3 km/h");
        // The caller shows the original when it gets null, which beats
        // showing a player the text "%number_a%"
        assertNull(template.restore("Vitesse: km/h"));
    }

    public void test_text_without_numbers_is_untouched() {
        NumericTemplate template = NumericTemplate.of("The server is restarting");
        assertFalse(template.isTemplated());
        assertEquals("The server is restarting", template.template());
        assertEquals("Le serveur redemarre", template.restore("Le serveur redemarre"));
    }

    public void test_a_message_that_is_only_numbers_has_nothing_to_say() {
        // An action bar of "12.3" costs a call for no gain
        assertFalse(NumericTemplate.of("12.3").hasWordsLeft());
        assertFalse(NumericTemplate.of("14:05:22").hasWordsLeft());
        assertTrue(NumericTemplate.of("Speed: 12.3").hasWordsLeft());
    }

    public void test_a_coordinate_dump_is_left_alone() {
        // More numbers than names, so the odds of losing one in translation
        // outweigh the saving
        NumericTemplate template = NumericTemplate.of("1 2 3 4 5 6 7 8 9 10");
        assertFalse(template.isTemplated());
        assertEquals("1 2 3 4 5 6 7 8 9 10", template.template());
    }

    public void test_null_and_empty_do_not_throw() {
        assertFalse(NumericTemplate.of(null).isTemplated());
        assertFalse(NumericTemplate.of("").isTemplated());
        assertNull(NumericTemplate.of("Speed: 1").restore(null));
    }

    public void test_a_number_touching_a_colour_code_still_lifts_out() {
        // Legacy section codes are how these messages actually arrive
        NumericTemplate template = NumericTemplate.of("§aSpeed: §f12.3");
        assertEquals("§aSpeed: §f%number_a%", template.template());
        assertEquals("§aVitesse: §f12.3",
                template.restore("§aVitesse: §f%number_a%"));
    }

    public void test_a_colour_codes_digit_is_not_a_number() {
        // The 6 in §6 is a colour, and templating it shipped §%number_a% to
        // the provider and buried the museum action bar. Found live.
        NumericTemplate template = NumericTemplate.of("§6§lUse /musehub to quit");
        assertFalse(template.isTemplated());
        assertEquals("§6§lUse /musehub to quit", template.template());
    }

    public void test_a_number_directly_after_a_colour_code_still_counts() {
        // In §612 the code is §6 and the 12 is real text
        NumericTemplate template = NumericTemplate.of("§6§l12 points");
        assertTrue(template.isTemplated());
        assertEquals("§6§l%number_a% points", template.template());
        assertEquals("§6§l12 punkte", template.restore("§6§l%number_a% punkte"));
    }
}
