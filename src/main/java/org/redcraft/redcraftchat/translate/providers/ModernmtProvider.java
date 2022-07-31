package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;

import com.modernmt.ModernMT;
import com.modernmt.model.Translation;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;

public class ModernmtProvider {

    private static ModernMT mmt = new ModernMT(Config.modernMtToken);

    private ModernmtProvider() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static Translation translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        String sourceLangId = sourceLanguageId.toLowerCase().split("-")[0];
        String targetLangId = targetLanguageId.toLowerCase().split("-")[0];

        String cacheKey = String.format("%s;%s;%s", sourceLangId, targetLangId, text);

        Translation cachedModernmtResponse = (Translation) CacheManager.get(CacheCategory.MODERNMT_TRANSLATED_MESSAGE, cacheKey, Translation.class);

        if (cachedModernmtResponse != null) {
            return cachedModernmtResponse;
        }

        Translation modernmtResponse = mmt.translate(sourceLangId, targetLangId, text);

        CacheManager.put(CacheCategory.MODERNMT_TRANSLATED_MESSAGE, cacheKey, modernmtResponse);

        // TODO remove debug
        RedCraftChat.getInstance().getLogger().info("Used " + modernmtResponse.getBilledCharacters() + " ModernMT PAID chars to translate to " + targetLangId);

        return modernmtResponse;
    }
}
