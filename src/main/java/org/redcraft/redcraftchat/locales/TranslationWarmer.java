package org.redcraft.redcraftchat.locales;

import java.util.List;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.translate.TranslationManager;

/**
 * Translates every interface string into every supported language once, so the
 * menus are served from the cache instead of being translated while a player
 * waits for them.
 *
 * The cache has no expiry, so this costs a single pass the first time a string
 * and a language meet, and nothing on any later boot.
 */
public class TranslationWarmer implements Runnable {

    @Override
    public void run() {
        RedCraftChat plugin = RedCraftChat.getInstance();

        if (!Config.translationEnabled) {
            plugin.getLogger().info("Not pre-translating the interface, translation is disabled");
            return;
        }

        String sourceLanguage = Config.defaultLocale.split("-")[0].toLowerCase();

        List<SupportedLocale> locales;
        try {
            locales = LocaleManager.getSupportedLocales();
        } catch (Exception e) {
            plugin.getLogger().warn("Could not pre-translate the interface, the supported locales are unavailable", e);
            return;
        }

        int translated = 0;
        int failed = 0;
        int skipped = 0;

        for (SupportedLocale locale : locales) {
            String targetLanguage = locale.code.split("-")[0].toLowerCase();

            if (targetLanguage.equals(sourceLanguage)) {
                continue;
            }

            // A fresh manager per language, the provider is picked in its
            // constructor and holds no per message state worth reusing
            TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

            for (String uiString : UiStrings.ALL) {
                if (!needsWarming(uiString, targetLanguage)) {
                    skipped++;
                    continue;
                }
                try {
                    translationManager.translate(uiString, sourceLanguage, targetLanguage);
                    translated++;
                } catch (Exception e) {
                    // One string failing must not stop the rest, the menus fall
                    // back to translating it live
                    failed++;
                }
            }
        }

        String summary = String.format(
                "Pre-translated %d interface strings (%d failed, %d already translated by hand)",
                translated, failed, skipped);
        plugin.getLogger().info(summary);
    }

    /**
     * Whether a string still has to be paid for in [targetLanguage].
     *
     * A hand-written translation is served straight out of UiTranslations by
     * localizeUiForPlayer, which returns before it ever reaches the cache, so
     * warming that string buys a cache entry nothing will ever read. The
     * saving grows with every string translated by hand, which is the right
     * way round.
     *
     * @param targetLanguage the language part of a locale, lowercased
     */
    public static boolean needsWarming(String uiString, String targetLanguage) {
        return UiTranslations.lookup(uiString, targetLanguage) == null;
    }
}
