package org.redcraft.redcraftchat.displaykit;

import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * The chat half of the fallback ladder: everything here works on a 1.7.2
 * client. Blocking label localisation, so scheduler threads only.
 */
public class LanguageSelectorPrompt {

    private LanguageSelectorPrompt() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * The first-join prompt for clients that cannot render the surface: an
     * explanation line, then the existing menu invitation, then a one-click
     * "keep what I have" that confirms and ends the prompting.
     */
    public static void sendFirstJoinPrompt(Player player, PlayerPreferences preferences) {
        String prompt = PlayerPreferencesManager.localizeMessageForPlayer(preferences, UiStrings.SELECTOR_JOIN_PROMPT);
        BasicMessageFormatter.sendInternalMessage(player,
 prompt, NamedTextColor.GREEN);

        String endonym = currentEndonym(preferences);
        String keepTemplate = PlayerPreferencesManager.localizeMessageForPlayer(preferences,
                UiStrings.SELECTOR_KEEP_CURRENT);
        String keepLabel = keepTemplate.replace("%language%", endonym);

        Component keep = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(keepLabel, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("]", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.runCommand("/lang confirm"))
                .hoverEvent(HoverEvent.showText(Component.text("/lang confirm", NamedTextColor.AQUA)));

        Component choose = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("/lang", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("]", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.runCommand("/lang"));

        player.sendMessage(Component.empty().append(keep).append(Component.text(" ")).append(choose));
    }

    /** The one-liner sent alongside the surface so nobody misses it. */
    public static void sendSurfaceHint(Player player, PlayerPreferences preferences) {
        String hint = PlayerPreferencesManager.localizeMessageForPlayer(preferences, UiStrings.SELECTOR_APPEARED);
        BasicMessageFormatter.sendInternalMessage(player,
 LegacyText.AQUA + hint, NamedTextColor.GREEN);
    }

    private static String currentEndonym(PlayerPreferences preferences) {
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            if (locale.code.equalsIgnoreCase(preferences.mainLanguage)) {
                return LocaleManager.getEndonym(locale);
            }
        }
        return preferences.mainLanguage;
    }
}
