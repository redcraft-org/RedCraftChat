package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.Normalizer.Form;

import com.deepl.api.DeepLException;
import com.deepl.api.Formality;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import com.deepl.api.Translator;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;

public class DeeplProvider implements TranslationProvider {

    private Translator translator;

    public DeeplProvider() {
        this.translator = new Translator(Config.deeplToken);
    }

    public String translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        String sourceLangId = sourceLanguageId.toLowerCase().split("-")[0];
        String targetLangId = targetLanguageId.toLowerCase();
        if (targetLangId.equals("en")) {
            targetLangId = "en-US";
        }
        String cacheKey = String.format("%s;%s;%s", sourceLangId, targetLangId, text);

        String cachedDeeplResponse = (String) CacheManager.get(CacheCategory.DEEPL_TRANSLATED_MESSAGE, cacheKey, String.class);

        if (cachedDeeplResponse != null) {
            return cachedDeeplResponse;
        }

        TextTranslationOptions translationOptions = new TextTranslationOptions();

        switch (Config.deeplFormality) {
            case "less":
                translationOptions.setFormality(Formality.PreferLess);
                break;

            case "more":
                translationOptions.setFormality(Formality.PreferMore);
                break;

            default:
                break;
        }

        translationOptions.setPreserveFormatting(Config.deeplPreserveFormatting);

        TextResult result;
        try {
            result = translator.translateText(text, sourceLangId, targetLangId, translationOptions);
        } catch (DeepLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not translate text from " + sourceLangId + " to "  + targetLangId + " with Deepl: " + e.getMessage());
        }

        // TODO remove debug
        String debugMessage = "Used " + text.length() + " Deepl chars to translate to " + targetLangId;
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        String translated = result.getText();

        CacheManager.put(CacheCategory.DEEPL_TRANSLATED_MESSAGE, cacheKey, translated);

        return translated;
    }
}
