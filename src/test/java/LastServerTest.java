import junit.framework.*;

import org.redcraft.redcraftchat.listeners.minecraft.MinecraftLastServerListener;

/**
 * When the selector should offer a way back, and when offering one would be
 * noise or a lie.
 */
public class LastServerTest extends TestCase {

    private String target(String last, String current) {
        return MinecraftLastServerListener.returnTargetName(last, current);
    }

    public void test_the_ordinary_case() {
        // On the hub, having come from survival
        assertEquals("survival_vanilla", target("survival_vanilla", "hub"));
    }

    public void test_nothing_recorded_yet_offers_nothing() {
        // A player who has never moved, so their first visit gets a plain list
        assertNull(target(null, "hub"));
        assertNull(target("", "hub"));
    }

    public void test_it_never_offers_the_server_you_are_standing_on() {
        // The button would do nothing, and reads as though the plugin has
        // lost track of where the player is
        assertNull(target("hub", "hub"));
    }

    public void test_an_unknown_current_server_still_offers_the_way_back() {
        // getCurrentServer is empty in the gap between backends, and during
        // that gap the recorded server is still the right answer
        assertEquals("museum", target("museum", null));
    }
}
