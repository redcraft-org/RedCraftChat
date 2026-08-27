import junit.framework.*;

import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager.SelectorRoute;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorManager.Trigger;

/**
 * The whole fallback contract in one truth table: the surface only ever
 * appears when every capability holds, and nothing below it ever loses the
 * chat path.
 */
public class LanguageSelectorRouteTest extends TestCase {

    private SelectorRoute route(boolean enabled, boolean lib, boolean client, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, lib, client, confirmed, trigger);
    }

    public void test_confirmed_players_are_left_alone_on_join() {
        assertEquals(SelectorRoute.NONE, route(true, true, true, true, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.NONE, route(false, false, false, true, Trigger.FIRST_JOIN));
    }

    public void test_unconfirmed_modern_client_gets_the_surface() {
        assertEquals(SelectorRoute.SURFACE_FIRST_JOIN, route(true, true, true, false, Trigger.FIRST_JOIN));
    }

    public void test_every_missing_capability_falls_to_the_chat_prompt() {
        assertEquals(SelectorRoute.CHAT_PROMPT, route(true, false, true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_PROMPT, route(true, true, false, false, Trigger.FIRST_JOIN));
    }

    /**
     * The config flag is a kill switch for the feature, not for the surface
     * alone: turning it off has to restore exactly what players saw before
     * the selector existed, which is no join prompt of any kind.
     */
    public void test_the_config_flag_silences_the_chat_prompt_too() {
        assertEquals(SelectorRoute.NONE, route(false, true, true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.NONE, route(false, false, false, false, Trigger.FIRST_JOIN));
        // and /lang keeps working, on the pre-existing chat menu
        assertEquals(SelectorRoute.CHAT_MENU, route(false, true, true, false, Trigger.LANG_NO_ARGS));
    }

    public void test_lang_without_args_prefers_the_surface_when_capable() {
        assertEquals(SelectorRoute.SURFACE_MANAGE, route(true, true, true, true, Trigger.LANG_NO_ARGS));
        assertEquals(SelectorRoute.SURFACE_MANAGE, route(true, true, true, false, Trigger.LANG_NO_ARGS));
    }

    public void test_lang_without_args_falls_to_the_chat_menu() {
        assertEquals(SelectorRoute.CHAT_MENU, route(false, true, true, true, Trigger.LANG_NO_ARGS));
        assertEquals(SelectorRoute.CHAT_MENU, route(true, false, true, true, Trigger.LANG_NO_ARGS));
        assertEquals(SelectorRoute.CHAT_MENU, route(true, true, false, true, Trigger.LANG_NO_ARGS));
    }

    public void test_arguments_always_keep_legacy_semantics() {
        assertEquals(SelectorRoute.CHAT_MENU, route(true, true, true, false, Trigger.LANG_WITH_ARGS));
        assertEquals(SelectorRoute.CHAT_MENU, route(false, false, false, true, Trigger.LANG_WITH_ARGS));
    }
}
