package org.redcraft.redcraftchat.locales;

import java.io.IOException;
import java.util.List;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.locales.providers.DatabaseLocaleProvider;
import org.redcraft.redcraftchat.locales.providers.RedCraftApiLocaleProvider;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;

public class LocaleManager {

    private static DatabaseLocaleProvider localeProvider;

    public static DatabaseLocaleProvider getLocaleProvider() {
        if (localeProvider == null) {
            switch (Config.supportedLocalesProvider) {
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
        List<SupportedLocale> supportedLocales = (List<SupportedLocale>) CacheManager.get(CacheCategory.SUPPORTED_LOCALES, Config.supportedLocalesProvider, List.class);

        if (supportedLocales != null) {
            return supportedLocales;
        }

        try {
            supportedLocales = getLocaleProvider().getSupportedLocales();
            CacheManager.put(CacheCategory.SUPPORTED_LOCALES, Config.supportedLocalesProvider, supportedLocales);
        } catch (IOException | InterruptedException e) {
            RedCraftChat.getInstance().getLogger().severe("Failed to get supported locales");
            e.printStackTrace();
        }

        return supportedLocales;
    }

    public static boolean isSupportedLocale(String locale) {
        return getSupportedLocales().stream().anyMatch(l -> l.code.equals(locale));
    }
}
