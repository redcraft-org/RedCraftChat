package org.redcraft.redcraftchat.locales.providers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.models.locales.SupportedLocale;

public class StaticLocaleProvider implements LocaleProvider {

    @Override
    public List<SupportedLocale> getSupportedLocales() throws IOException, InterruptedException {
        List<SupportedLocale> locales = new ArrayList<SupportedLocale>();
        locales.add(new SupportedLocale("en-US", "English"));
        locales.add(new SupportedLocale("fr-FR", "French"));
        return locales;
    }
}
