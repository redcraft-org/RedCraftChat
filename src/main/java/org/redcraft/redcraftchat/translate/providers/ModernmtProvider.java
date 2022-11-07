package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;

import com.modernmt.ModernMT;
import com.modernmt.model.Translation;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;

public class ModernmtProvider implements TranslationProvider {

    private ModernMT mmt;

    public ModernmtProvider() {
        mmt = new ModernMT(Config.modernMtToken);
    }

    public String translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        String sourceLangId = sourceLanguageId.toLowerCase().split("-")[0];
        String targetLangId = targetLanguageId.toLowerCase().split("-")[0];

        String cacheKey = String.format("%s;%s;%s", sourceLangId, targetLangId, text);

        String cachedModernmtResponse = (String) CacheManager.get(CacheCategory.MODERNMT_TRANSLATED_MESSAGE, cacheKey, String.class);

        if (cachedModernmtResponse != null) {
            return cachedModernmtResponse;
        }

        Translation modernmtResponse = mmt.translate(sourceLangId, targetLangId, text);

        // TODO remove debug
        String debugMessage = "Used " + modernmtResponse.getBilledCharacters() + " ModernMT PAID chars to translate to " + targetLangId;
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        String translated = modernmtResponse.getTranslation().replaceAll("§( )+", "§");

        CacheManager.put(CacheCategory.MODERNMT_TRANSLATED_MESSAGE, cacheKey, translated);

        return translated;
    }
}
