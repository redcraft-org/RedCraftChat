package org.redcraft.redcraftbungeechat.translate;
import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.models.deepl.DeeplResponse;
import org.redcraft.redcraftbungeechat.models.translate.TokenizedMessage;
import org.redcraft.redcraftbungeechat.tokenizer.TokenizerManager;
import org.redcraft.redcraftbungeechat.translate.services.DeeplClient;

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
