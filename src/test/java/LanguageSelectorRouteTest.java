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

    /** No dialog support: the client predates 1.21.6. */
    private SelectorRoute route(boolean enabled, boolean lib, boolean client, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, false, lib, client, false, confirmed, trigger);
    }

    private SelectorRoute withDialog(boolean enabled, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, true, true, true, false, confirmed, trigger);
    }

    /** Bedrock, reported by Geyser as a modern Java client. */
    private SelectorRoute bedrock(boolean enabled, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, true, true, true, true, confirmed, trigger);
    }

    /** Bedrock on a proxy with no dialog support to offer it. */
    private SelectorRoute bedrockNoDialog(boolean enabled, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, false, true, true, true, confirmed, trigger);
    }

    public void test_bedrock_gets_the_dialog_like_everyone_else() {
        // Geyser translates a show_dialog into its own form, inputs and all,
        // and sends the custom_click_action back, so the round trip works
        // there unchanged. This was previously routed to a parallel Cumulus
        // implementation on the belief that it could not.
        assertEquals(SelectorRoute.DIALOG_FIRST_JOIN, bedrock(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.DIALOG_MANAGE, bedrock(true, true, Trigger.LANG_NO_ARGS));
        assertEquals(SelectorRoute.DIALOG_MANAGE, bedrock(true, false, Trigger.LANG_NO_ARGS));
    }

    public void test_bedrock_never_gets_the_in_world_panel() {
        // The panel is display entities driven by interaction packets, which
        // Geyser does not put in front of a Bedrock player. Without a dialog
        // to fall back on, that leaves chat rather than the panel.
        assertEquals(SelectorRoute.CHAT_PROMPT, bedrockNoDialog(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, bedrockNoDialog(true, true, Trigger.LANG_NO_ARGS));
    }

    public void test_bedrock_still_obeys_the_other_rules() {
        assertEquals(SelectorRoute.NONE, bedrock(true, true, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.NONE, bedrock(false, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, bedrock(true, false, Trigger.LANG_WITH_ARGS));
    }

    public void test_java_is_untouched_by_the_bedrock_rule() {
        // The same inputs with bedrock=false must still reach the dialog
        assertEquals(SelectorRoute.DIALOG_FIRST_JOIN, withDialog(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.DIALOG_MANAGE, withDialog(true, true, Trigger.LANG_NO_ARGS));
    }

    public void test_the_dialog_wins_wherever_the_client_can_show_one() {
        // Its floor is lower than the panel's, so it covers strictly more
        assertEquals(SelectorRoute.DIALOG_FIRST_JOIN, withDialog(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.DIALOG_MANAGE, withDialog(true, true, Trigger.LANG_NO_ARGS));
    }

    public void test_the_dialog_does_not_need_displaykit() {
        // The client draws it, so a missing or broken library is irrelevant
        assertEquals(SelectorRoute.DIALOG_FIRST_JOIN,
                LanguageSelectorManager.decide(true, true, false, false, false, false, Trigger.FIRST_JOIN));
    }

    public void test_the_kill_switch_still_covers_the_dialog() {
        assertEquals(SelectorRoute.NONE, withDialog(false, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, withDialog(false, true, Trigger.LANG_NO_ARGS));
    }

    public void test_arguments_stay_on_chat_even_with_a_dialog() {
        assertEquals(SelectorRoute.CHAT_MENU, withDialog(true, false, Trigger.LANG_WITH_ARGS));
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
