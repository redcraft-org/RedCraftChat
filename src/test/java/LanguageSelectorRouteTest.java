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
        return LanguageSelectorManager.decide(enabled, false, lib, client, false, false, confirmed, trigger);
    }

    private SelectorRoute withDialog(boolean enabled, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, true, true, true, false, false, confirmed, trigger);
    }

    /** Every capability reports true, because Geyser says so, and all of them lie. */
    private SelectorRoute bedrock(boolean enabled, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, true, true, true, true, false, confirmed, trigger);
    }

    public void test_bedrock_never_gets_a_surface_it_cannot_draw() {
        // The regression test for the live bug. Geyser presents Bedrock as a
        // modern Java client, so dialogSupported and clientSupported are both
        // true here and both are wrong: it renders neither, and the dialog's
        // reply never arrives, so the player waits on nothing.
        assertEquals(SelectorRoute.CHAT_PROMPT, bedrock(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, bedrock(true, true, Trigger.LANG_NO_ARGS));
        assertEquals(SelectorRoute.CHAT_MENU, bedrock(true, false, Trigger.LANG_NO_ARGS));
    }

    public void test_bedrock_still_obeys_the_other_rules() {
        // Confirmed players are left alone, and the kill switch still kills
        assertEquals(SelectorRoute.NONE, bedrock(true, true, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.NONE, bedrock(false, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, bedrock(true, false, Trigger.LANG_WITH_ARGS));
    }

    /** Bedrock with Floodgate present: its own UI, not a text fallback. */
    private SelectorRoute bedrockWithForms(boolean enabled, boolean confirmed, Trigger trigger) {
        return LanguageSelectorManager.decide(enabled, true, true, true, true, true, confirmed, trigger);
    }

    public void test_bedrock_gets_its_own_ui_when_floodgate_is_there() {
        assertEquals(SelectorRoute.FORM_FIRST_JOIN, bedrockWithForms(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.FORM_MANAGE, bedrockWithForms(true, true, Trigger.LANG_NO_ARGS));
    }

    public void test_bedrock_falls_to_chat_without_floodgate() {
        // The form is the good path, not the only one: a proxy without
        // Floodgate still has to leave Bedrock players able to choose
        assertEquals(SelectorRoute.CHAT_PROMPT, bedrock(true, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, bedrock(true, true, Trigger.LANG_NO_ARGS));
    }

    public void test_the_kill_switch_covers_the_form_too() {
        assertEquals(SelectorRoute.NONE, bedrockWithForms(false, false, Trigger.FIRST_JOIN));
        assertEquals(SelectorRoute.CHAT_MENU, bedrockWithForms(false, true, Trigger.LANG_NO_ARGS));
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
                LanguageSelectorManager.decide(true, true, false, false, false, false, false, Trigger.FIRST_JOIN));
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
