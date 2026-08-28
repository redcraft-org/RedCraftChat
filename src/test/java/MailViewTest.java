import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import junit.framework.*;

import org.redcraft.redcraftchat.messaging.MailView;
import org.redcraft.redcraftchat.models.players.PlayerMail;

/**
 * Slots are what make a single mail reachable by typing. They are short and
 * per page, which means they must never resolve against a page the player has
 * already left: the actions behind a slot mark mail read and send replies.
 */
public class MailViewTest extends TestCase {

    private PlayerMail mail(String id) {
        PlayerMail m = new PlayerMail();
        m.internalId = id;
        return m;
    }

    private List<PlayerMail> page(String... ids) {
        List<PlayerMail> out = new ArrayList<>();
        for (String id : ids) {
            out.add(mail(id));
        }
        return out;
    }

    public void testSlotsAreOneBasedAndMapToInternalIds() {
        MailView view = MailView.of(UUID.randomUUID());
        view.record(1, true, page("101", "102", "103"));

        assertEquals("101", view.internalId(1));
        assertEquals("102", view.internalId(2));
        assertEquals("103", view.internalId(3));
    }

    public void testSlotsOutsideThePageDoNotResolve() {
        MailView view = MailView.of(UUID.randomUUID());
        view.record(1, true, page("101", "102"));

        assertNull(view.internalId(0));
        assertNull(view.internalId(3));
        assertNull(view.internalId(-1));
        assertNull(view.internalId(99));
    }

    public void testAStaleSlotMissesRatherThanHittingTheWrongMail() {
        // The whole reason slots are recorded per page. Turning to a shorter
        // page must not leave slot 3 pointing at somebody else's mail.
        MailView view = MailView.of(UUID.randomUUID());
        view.record(1, true, page("101", "102", "103"));
        assertEquals("103", view.internalId(3));

        view.record(2, true, page("201"));
        assertEquals("201", view.internalId(1));
        assertNull("slot 3 must not survive a shorter page", view.internalId(3));
    }

    public void testTheViewRemembersWhereThePlayerIs() {
        MailView view = MailView.of(UUID.randomUUID());
        view.record(3, false, page("1"));
        assertEquals(3, view.page());
        assertFalse(view.unreadOnly());
    }

    public void testSlotNumbersTrackWhatIsOnScreen() {
        MailView view = MailView.of(UUID.randomUUID());
        view.record(1, true, page("a", "b"));
        assertEquals(Arrays.asList("1", "2"), view.slotNumbers());
        view.record(1, true, page());
        assertTrue(view.slotNumbers().isEmpty());
    }

    public void testForgettingDropsTheView() {
        UUID id = UUID.randomUUID();
        MailView.of(id).record(1, true, page("101"));
        MailView.forget(id);
        assertNull("a fresh view must not carry the old page", MailView.of(id).internalId(1));
    }
}
