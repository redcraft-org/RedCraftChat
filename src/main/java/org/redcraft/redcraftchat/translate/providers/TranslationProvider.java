package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;

public interface TranslationProvider {
    public String translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException;
}
