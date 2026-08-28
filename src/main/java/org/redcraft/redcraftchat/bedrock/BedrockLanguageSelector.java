package org.redcraft.redcraftchat.bedrock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * The language selector as a native Bedrock form.
 *
 * Bedrock cannot use any of the other surfaces. Chat components reach it
 * through Geyser's legacy serialiser, so clicks and hovers are gone; the Java
 * dialog is a packet Geyser does not translate; display entities do not
 * render. What it does have is its own UI, and Floodgate can drive it.
 *
 * The two steps mirror the Java dialog exactly: a list of buttons to pick the
 * main language, then a set of toggles for the rest. Forms carry their own
 * response callbacks, so unlike the Java dialog there is no packet listener
 * and no id round trip to get wrong.
 *
 * Every handler hops to a scheduler thread: the callback arrives on Geyser's
 * thread and everything here blocks on preferences or translation.
 */
public final class BedrockLanguageSelector {

    private BedrockLanguageSelector() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static boolean isSupported(Player player) {
        return player != null
                && org.redcraft.redcraftchat.minecraft.BedrockPlayers.isBedrock(player)
                && BedrockForms.isAvailable();
    }

    /** Step one: one button per language, the current one marked. */
    public static boolean showPrimary(Player player, PlayerPreferences preferences) {
        if (!isSupported(player)) {
            return false;
        }

        List<SupportedLocale> locales = supportedLocales();
        if (locales.isEmpty()) {
            return false;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title(ui(preferences, UiStrings.SELECTOR_PRIMARY_TITLE))
                .content(ui(preferences, UiStrings.SELECTOR_PRIMARY_HELP));

        for (SupportedLocale locale : locales) {
            boolean isMain = locale.code.equalsIgnoreCase(preferences.mainLanguage);
            form.button(isMain
                    ? LocaleManager.getEndonym(locale) + " ✔"
                    : LocaleManager.getEndonym(locale));
        }

        UUID playerId = player.getUniqueId();
        form.validResultHandler(response -> onPrimaryChosen(playerId, locales, response.clickedButtonId()));
        // A player who dismisses the form has still not chosen, so they are
        // left unconfirmed and prompted again next session rather than being
        // silently treated as done
        form.closedOrInvalidResultHandler(() -> { });

        return BedrockForms.send(playerId, form);
    }

    /** Step two: a toggle per language, the main one excluded. */
    public static boolean showOthers(Player player, PlayerPreferences preferences) {
        if (!isSupported(player)) {
            return false;
        }

        List<SupportedLocale> others = new ArrayList<>();
        for (SupportedLocale locale : supportedLocales()) {
            if (!locale.code.equalsIgnoreCase(preferences.mainLanguage)) {
                others.add(locale);
            }
        }

        CustomForm.Builder form = CustomForm.builder()
                .title(ui(preferences, UiStrings.SELECTOR_OTHERS_TITLE))
                .label(ui(preferences, UiStrings.SELECTOR_OTHERS_HELP));

        for (SupportedLocale locale : others) {
            boolean understood = preferences.languages != null
                    && preferences.languages.contains(locale.code);
            form.toggle(LocaleManager.getEndonym(locale), understood);
        }

        UUID playerId = player.getUniqueId();
        form.validResultHandler(response -> onOthersSubmitted(playerId, others, response));
        form.closedOrInvalidResultHandler(() -> { });

        return BedrockForms.send(playerId, form);
    }

    private static void onPrimaryChosen(UUID playerId, List<SupportedLocale> locales, int buttonId) {
        onScheduler(playerId, player -> {
            if (buttonId < 0 || buttonId >= locales.size()) {
                return;
            }
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            PlayerPreferencesManager.setMainPlayerLocale(preferences, locales.get(buttonId).code);
            // Re-read so the second step sees the language list the mutation
            // just grew, and localises into the language just chosen
            showOthers(player, PlayerPreferencesManager.getPlayerPreferences(player));
        });
    }

    private static void onOthersSubmitted(UUID playerId, List<SupportedLocale> others,
            org.geysermc.cumulus.response.CustomFormResponse response) {
        onScheduler(playerId, player -> {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

            for (int i = 0; i < others.size(); i++) {
                SupportedLocale locale = others.get(i);
                // The label occupies index 0, so the toggles start at 1
                Boolean wanted = readToggle(response, i + 1);
                if (wanted == null) {
                    continue;
                }
                boolean current = preferences.languages != null
                        && preferences.languages.contains(locale.code);
                if (wanted != current) {
                    try {
                        PlayerPreferencesManager.togglePlayerLocale(
                                PlayerPreferencesManager.getPlayerPreferences(player), locale.code);
                    } catch (Exception e) {
                        RedCraftChat.getInstance().getLogger().warn("Could not toggle {} for {}: {}",
                                locale.code, player.getUsername(), e.getMessage());
                    }
                }
            }

            PlayerPreferences confirmed = PlayerPreferencesManager.getPlayerPreferences(player);
            PlayerPreferencesManager.confirmLanguageSelection(confirmed);
            BasicMessageFormatter.sendInternalMessage(player,
                    PlayerPreferencesManager.localizeUiForPlayer(confirmed, UiStrings.SELECTOR_CONFIRMED),
                    NamedTextColor.GREEN);
        });
    }

    private static Boolean readToggle(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
        try {
            Object value = response.valueAt(index);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Exception e) {
            // A form the client filled differently than we built is not an
            // answer we can act on
            return null;
        }
    }

    private interface PlayerTask {
        void run(Player player) throws Exception;
    }

    /** Form callbacks arrive on Geyser's thread; everything here blocks. */
    private static void onScheduler(UUID playerId, PlayerTask task) {
        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            Player player = plugin.getProxy().getPlayer(playerId).orElse(null);
            if (player == null) {
                return;
            }
            try {
                task.run(player);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Bedrock language form failed for {}: {}",
                        player.getUsername(), e.getMessage());
            }
        }).schedule();
    }

    private static List<SupportedLocale> supportedLocales() {
        try {
            List<SupportedLocale> locales = LocaleManager.getSupportedLocales();
            return locales == null ? new ArrayList<>() : locales;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String ui(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }
}
