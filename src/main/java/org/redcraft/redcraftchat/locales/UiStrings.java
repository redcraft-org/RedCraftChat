package org.redcraft.redcraftchat.locales;

import java.util.Arrays;
import java.util.List;

/**
 * Every fixed piece of interface text the plugin shows to a player.
 *
 * These are translated through the same path as chat, so the first player to
 * open a menu in a language used to pay for the translation and watch the menu
 * arrive half translated. Listing them here lets them all be translated up front
 * into every supported language, see {@link TranslationWarmer}.
 *
 * A string that is missing from this list still works, it is just translated on
 * the first player who sees it rather than ahead of time.
 */
public class UiStrings {

    public static final String LANGUAGE_SELECTOR_HEADER = "LANGUAGE SELECTOR";
    public static final String LANGUAGE_SELECTOR_HELP = "Click on a language to enable or disable it, click on the checkbox to make it default.";
    public static final String LEGEND = "Legend:";
    public static final String DISABLED = "disabled";
    public static final String ENABLED = "enabled";
    public static final String ALREADY_MAIN_LANGUAGE = "This is already your main language";
    public static final String SET_AS_MAIN_LANGUAGE = "Click to set as main language";
    public static final String ADD_TO_LANGUAGES = "Click to add this language";
    public static final String REMOVE_FROM_LANGUAGES = "Click to remove this languages";
    public static final String CHANGE_MAIN_LANGUAGE = "Change main language";

    public static final String MAIL_INBOX_HEADER = "MAIL INBOX";
    public static final String MAIL_NO_MAILS = "You have no mails.";
    public static final String MAIL_NEXT_PAGE = "Next page";
    public static final String MAIL_PREVIOUS_PAGE = "Previous page";
    public static final String MAIL_NEXT_PAGE_HOVER = "Click to go to the next page";
    public static final String MAIL_PREVIOUS_PAGE_HOVER = "Click to go to the previous page";
    public static final String MAIL_MARK_AS_READ = "Mark as read";
    public static final String MAIL_ALREADY_READ = "Already marked as read";
    public static final String MAIL_CLICK_TO_REPLY = "Click to reply";
    public static final String MAIL_HOVER_TIP = "Tip: Hover the message to see the full text";

    public static final String SELECTOR_PRIMARY_TITLE = "What is your primary language?";
    public static final String SELECTOR_OTHERS_TITLE = "Do you understand other languages?";
    public static final String SELECTOR_PRIMARY_HELP = "Everything on the server gets translated into it";
    public static final String SELECTOR_OTHERS_HELP = "Ticked languages reach you as written, never translated";
    public static final String SELECTOR_NEXT = "Next";
    public static final String SELECTOR_BACK = "Back";
    public static final String SELECTOR_CLOSE = "Close";
    public static final String SELECTOR_PREVIOUS = "Previous";
    public static final String SELECTOR_DONE = "Done";
    public static final String SELECTOR_JOIN_PROMPT = "Please confirm the language you want to play in";
    public static final String SELECTOR_KEEP_CURRENT = "Keep %language%";
    public static final String SELECTOR_APPEARED = "A language selector appeared in front of you";
    public static final String SELECTOR_CONFIRMED = "Language confirmed, have fun!";

    public static final String DISCORD_COPY_TO_CLIPBOARD = "Copy to clipboard";
    public static final String DISCORD_RUN_COMMAND = "Please run the following command on our Discord server (click to copy): ";
    public static final String DISCORD_UNLINK = "Unlink";
    public static final String DISCORD_UNLINK_ACCOUNT = "Unlink your Discord account";
    public static final String DISCORD_ALREADY_LINKED = "You already linked your Discord account. If you wish to unlink it, click on the button below";

    public static final List<String> ALL = Arrays.asList(
            LANGUAGE_SELECTOR_HEADER,
            LANGUAGE_SELECTOR_HELP,
            LEGEND,
            DISABLED,
            ENABLED,
            ALREADY_MAIN_LANGUAGE,
            SET_AS_MAIN_LANGUAGE,
            ADD_TO_LANGUAGES,
            REMOVE_FROM_LANGUAGES,
            CHANGE_MAIN_LANGUAGE,
            SELECTOR_PRIMARY_TITLE,
            SELECTOR_OTHERS_TITLE,
            SELECTOR_PRIMARY_HELP,
            SELECTOR_OTHERS_HELP,
            SELECTOR_NEXT,
            SELECTOR_BACK,
            SELECTOR_CLOSE,
            SELECTOR_PREVIOUS,
            SELECTOR_DONE,
            SELECTOR_JOIN_PROMPT,
            SELECTOR_KEEP_CURRENT,
            SELECTOR_APPEARED,
            SELECTOR_CONFIRMED,
            MAIL_INBOX_HEADER,
            MAIL_NO_MAILS,
            MAIL_NEXT_PAGE,
            MAIL_PREVIOUS_PAGE,
            MAIL_NEXT_PAGE_HOVER,
            MAIL_PREVIOUS_PAGE_HOVER,
            MAIL_MARK_AS_READ,
            MAIL_ALREADY_READ,
            MAIL_CLICK_TO_REPLY,
            MAIL_HOVER_TIP,
            DISCORD_COPY_TO_CLIPBOARD,
            DISCORD_RUN_COMMAND,
            DISCORD_UNLINK,
            DISCORD_UNLINK_ACCOUNT,
            DISCORD_ALREADY_LINKED);

    private UiStrings() {
    }
}
