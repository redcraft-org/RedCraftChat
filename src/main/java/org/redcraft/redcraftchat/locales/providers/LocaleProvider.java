package org.redcraft.redcraftchat.locales.providers;

import java.io.IOException;
import java.util.List;

import org.redcraft.redcraftchat.models.locales.SupportedLocale;

public interface LocaleProvider {
    public List<SupportedLocale> getSupportedLocales() throws IOException, InterruptedException;
}
