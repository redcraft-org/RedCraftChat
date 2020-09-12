package org.redcraft.redcraftbungeechat.translate;

import org.redcraft.redcraftbungeechat.models.deepl.DeeplResponse;
import org.redcraft.redcraftbungeechat.translate.services.DeeplClient;

public class TranslationManager {
    public static String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        // TODO actual translation manager instead of just passing through to Deepl

        DeeplResponse dr = DeeplClient.translate(text, sourceLanguage, targetLanguage);
        return DeeplClient.parseDeeplResponse(dr);
    }
}
