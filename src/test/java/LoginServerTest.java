import junit.framework.*;

import org.redcraft.redcraftchat.servers.LoginServer;

/**
 * Three answers share one column, so the whole contract is which of them a
 * stored pair of values means.
 */
public class LoginServerTest extends TestCase {

    public void test_unset_leaves_the_choice_to_the_proxy() {
        // The default, and what every player who never opens the setting has
        assertNull(LoginServer.resolve(null, "survival_vanilla"));
        assertNull(LoginServer.resolve("", "survival_vanilla"));
    }

    public void test_a_named_server_wins_over_where_they_were() {
        // The case this exists for: someone who plays redstone lands on
        // redstone whatever they did last session
        assertEquals("crea_redstone_plot",
                LoginServer.resolve("crea_redstone_plot", "survival_vanilla"));
    }

    public void test_last_means_where_they_left_off() {
        assertEquals("museum", LoginServer.resolve(LoginServer.LAST, "museum"));
    }

    public void test_last_with_nothing_recorded_falls_back_to_the_proxy() {
        // Picking "last server" before ever moving must not strand anyone
        assertNull(LoginServer.resolve(LoginServer.LAST, null));
        assertNull(LoginServer.resolve(LoginServer.LAST, ""));
    }

    public void test_the_unset_default_is_read_from_the_try_list() {
        // Whatever the proxy would have done, so the setting screen can tick
        // the real answer instead of offering a vague rule that only ever
        // meant "the hub"
        assertEquals("hub", LoginServer.effectiveDefault(
                java.util.Arrays.asList("hub", "survival_vanilla"),
                java.util.Arrays.asList("hub", "survival_vanilla", "museum")));
    }

    public void test_a_try_list_entry_the_proxy_dropped_is_skipped() {
        // A server can be removed from velocity.toml's server map and left in
        // the try list, and the next entry is what a player actually gets
        assertEquals("museum", LoginServer.effectiveDefault(
                java.util.Arrays.asList("gone", "museum"),
                java.util.Arrays.asList("hub", "museum")));
        assertNull(LoginServer.effectiveDefault(
                java.util.Arrays.asList("gone"), java.util.Arrays.asList("hub")));
        assertNull(LoginServer.effectiveDefault(null, java.util.Arrays.asList("hub")));
    }

    public void test_a_server_called_last_is_still_a_server() {
        // The sentinel carries a character a server id cannot, so a backend
        // named "last" is treated as a name and not as the rule
        assertEquals("last", LoginServer.resolve("last", "hub"));
        assertTrue(LoginServer.isFixedServer("last"));
        assertFalse(LoginServer.isFixedServer(LoginServer.LAST));
        assertFalse(LoginServer.isFixedServer(null));
    }
}
