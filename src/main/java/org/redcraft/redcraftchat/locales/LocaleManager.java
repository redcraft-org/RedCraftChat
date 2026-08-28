package org.redcraft.redcraftchat.locales;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.locales.providers.DatabaseLocaleProvider;
import org.redcraft.redcraftchat.locales.providers.LocaleProvider;
import org.redcraft.redcraftchat.locales.providers.RedCraftApiLocaleProvider;
import org.redcraft.redcraftchat.locales.providers.StaticLocaleProvider;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;

import com.google.common.reflect.TypeToken;

public class LocaleManager {

    private LocaleManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    private static LocaleProvider localeProvider;

    public static LocaleProvider getLocaleProvider() {
        if (localeProvider == null) {
            switch (Config.supportedLocalesProvider) {
                case "static":
                    localeProvider = new StaticLocaleProvider();
                    break;

                case "database":
                    localeProvider = new DatabaseLocaleProvider();
                    break;

                case "api":
                    localeProvider = new RedCraftApiLocaleProvider();
                    break;

                default:
                    throw new IllegalStateException("Unknown database player Provider: " + Config.supportedLocalesProvider);
            }
        }
        return localeProvider;
    }

    @SuppressWarnings("unchecked")
    public static List<SupportedLocale> getSupportedLocales() {
        List<SupportedLocale> supportedLocales = (List<SupportedLocale>) CacheManager.get(CacheCategory.SUPPORTED_LOCALES, Config.supportedLocalesProvider, new TypeToken<List<SupportedLocale>>() {}.getType());

        if (supportedLocales != null) {
            return supportedLocales;
        }

        try {
            supportedLocales = getLocaleProvider().getSupportedLocales();
            CacheManager.put(CacheCategory.SUPPORTED_LOCALES, Config.supportedLocalesProvider, supportedLocales);
        } catch (IOException | InterruptedException e) {
            RedCraftChat.getInstance().getLogger().error("Failed to get supported locales");
            e.printStackTrace();
        }

        return supportedLocales;
    }

    public static List<String> getSupportedLanguages() {
        List<String> supportedLanguages = new ArrayList<String>();

        for (SupportedLocale locale : getSupportedLocales()) {
            String strippedLocale = locale.code.split("-")[0];
            if (!supportedLanguages.contains(strippedLocale)) {
                supportedLanguages.add(strippedLocale);
            }
        }

        return supportedLanguages;
    }

    public static boolean isSupportedLocale(String locale) {
        List<SupportedLocale> locales = getSupportedLocales();
        if (locales == null) {
            return locale.equals(Config.defaultLocale);
        }
        return locales.stream().anyMatch(l -> l.code.equals(locale));
    }

    /**
     * The name of a language as its own speakers write it, so a French
     * speaker looks for "Francais" and not for whatever the reader's
     * language calls it. Falls back to the stored name when the JVM has no
     * display name for the tag.
     */
    public static String getEndonym(SupportedLocale locale) {
        java.util.Locale localeTag = java.util.Locale.forLanguageTag(locale.code.replace('_', '-'));
        String endonym = localeTag.getDisplayLanguage(localeTag);

        if (endonym.isEmpty() || endonym.equalsIgnoreCase(localeTag.getLanguage())) {
            return locale.name;
        }

        return endonym.substring(0, 1).toUpperCase(localeTag) + endonym.substring(1);
    }
}
