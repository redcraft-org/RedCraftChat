import java.time.LocalDateTime;

import junit.framework.*;

import org.redcraft.redcraftchat.messaging.MailMessagesManager;

/**
 * The date is the one part of a mail row that is not translated, so it has to
 * be right on its own. Big end first, and always carrying the year.
 */
public class MailDateFormatTest extends TestCase {

    public void test_the_order_is_year_month_day() {
        // Written any other way, 08/09 is two different days depending on
        // where the reader is from
        assertEquals("2026/08/27 14:32",
                MailMessagesManager.formatSentAt(LocalDateTime.of(2026, 8, 27, 14, 32)));
    }

    public void test_the_year_is_always_there() {
        // The reason this exists: players come back after years away, and a
        // bare day and month says nothing about which year it belongs to
        assertTrue(MailMessagesManager.formatSentAt(
                LocalDateTime.of(2021, 3, 4, 9, 5)).startsWith("2021/"));
    }

    public void test_the_last_days_of_december_stay_in_their_own_year() {
        // The pattern is yyyy, not YYYY. The capital is the week-based year,
        // which calls 2026/12/28 a day in 2027 and would date a mail into a
        // year that has not happened.
        assertEquals("2026/12/28 23:59",
                MailMessagesManager.formatSentAt(LocalDateTime.of(2026, 12, 28, 23, 59)));
        assertEquals("2026/12/31 12:00",
                MailMessagesManager.formatSentAt(LocalDateTime.of(2026, 12, 31, 12, 0)));
    }

    public void test_single_digits_are_padded() {
        // So every row is the same width and the column lines up
        assertEquals("2026/01/02 03:04",
                MailMessagesManager.formatSentAt(LocalDateTime.of(2026, 1, 2, 3, 4)));
    }

    public void test_a_mail_with_no_date_says_nothing_rather_than_lying() {
        assertNull(MailMessagesManager.formatSentAt(null));
    }
}
