package org.redcraft.redcraftchat.locales.providers;

import java.util.List;

import org.redcraft.redcraftchat.models.locales.SupportedLocale;

interface LocaleProviderInterface {
    public List<SupportedLocale> getSupportedLocales();
}
