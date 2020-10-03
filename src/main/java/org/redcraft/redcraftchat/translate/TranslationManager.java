package org.redcraft.redcraftchat.translate;
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
                DeeplResponse dr = DeeplClient.translate(tokenizedMessage.tokenizedMessage, sourceLanguage, targetLanguage);
                String translated = DeeplClient.parseDeeplResponse(dr);
                tokenizedMessage.tokenizedMessage = translated;
                return TokenizerManager.untokenizeElements(tokenizedMessage);
            default:
                throw new Exception(String.format("Unknown translation service \"%s\"", Config.translationService));
        }
    }
}
