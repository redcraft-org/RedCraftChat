package org.redcraft.redcraftchat.translate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.models.deepl.DeeplResponse;
import org.redcraft.redcraftchat.models.translate.TokenizedMessage;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;
import org.redcraft.redcraftchat.translate.services.DeeplClient;

public class TranslationManager {
    public static String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        if (!Config.translationEnabled) {
            throw new Exception("TranslationManager was called but translation is disabled in the configuration");
        }

        switch (Config.translationService) {
            case "deepl":
                TokenizedMessage tokenizedMessage = TokenizerManager.tokenizeElements(text, true);
                DeeplResponse dr = DeeplClient.translate(tokenizedMessage.tokenizedMessage, sourceLanguage.toUpperCase(), targetLanguage.toUpperCase());
                String translated = DeeplClient.parseDeeplResponse(dr);
                tokenizedMessage.tokenizedMessage = translated;
                return TokenizerManager.untokenizeElements(tokenizedMessage);
            default:
                throw new Exception(String.format("Unknown translation service \"%s\"", Config.translationService));
        }
    }

    // TODO parallelize
    public static Map<String, String> translateBulk(String text, String sourceLanguage, List<String> targetLanguages) {
        Map<String, String> translatedLanguages = new HashMap<String, String>();

        for (String targetLanguage : targetLanguages) {
            if (targetLanguage.equalsIgnoreCase(sourceLanguage)) {
                translatedLanguages.put(targetLanguage, text);
                continue;
            }
            try {
                translatedLanguages.put(targetLanguage, TranslationManager.translate(text, sourceLanguage, targetLanguage));
            } catch (Exception e) {
                translatedLanguages.put(targetLanguage, text);
                e.printStackTrace();
            }
        }

        return translatedLanguages;
    }
}
