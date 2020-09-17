package org.redcraft.redcraftbungeechat.translate;

import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.models.deepl.DeeplResponse;
import org.redcraft.redcraftbungeechat.translate.services.DeeplClient;

public class TranslationManager {
    public static String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        if (!Config.translationEnabled) {
            throw new Exception("TranslationManager was called but translation is disabled in the configuration");
        }

        switch (Config.translationService) {
            case "deepl":
                DeeplResponse dr = DeeplClient.translate(text, sourceLanguage, targetLanguage);
                return DeeplClient.parseDeeplResponse(dr);
            default:
                throw new Exception("Unknown translation service \"" + Config.translationService + "\"");
        }
    }
}
