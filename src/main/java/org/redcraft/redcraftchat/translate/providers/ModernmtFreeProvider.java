package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import org.apache.http.client.utils.URIBuilder;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.modernmt.ModernmtResponse;

public class ModernmtFreeProvider {

    private static HttpClient httpClient = HttpClient.newHttpClient();

    private ModernmtFreeProvider() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static ModernmtResponse translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        String sourceLangId = sourceLanguageId.toLowerCase().split("-")[0];
        String targetLangId = targetLanguageId.toLowerCase().split("-")[0];

        String cacheKey = String.format("%s;%s;%s", sourceLangId, targetLangId, text);

        ModernmtResponse cachedModernmtResponse = (ModernmtResponse) CacheManager.get(CacheCategory.MODERNMT_FREE_TRANSLATED_MESSAGE, cacheKey, ModernmtResponse.class);

        if (cachedModernmtResponse != null) {
            return cachedModernmtResponse;
        }

        URIBuilder url = new URIBuilder("https://webapi.modernmt.com/translate");
        url.addParameter("q", text);
        url.addParameter("source", sourceLangId);
        url.addParameter("target", targetLangId);


        HttpRequest request = HttpRequest.newBuilder(url.build())
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        ModernmtResponse modernmtResponse = new Gson().fromJson(response.body(), ModernmtResponse.class);

        // Remove special chars from modernmt response
        modernmtResponse.data.translation = modernmtResponse.data.translation.replace('\u00A0', ' ');
        modernmtResponse.data.translation = modernmtResponse.data.translation.replace("&lt;", "<");
        modernmtResponse.data.translation = modernmtResponse.data.translation.replace("&gt;", ">");

        CacheManager.put(CacheCategory.MODERNMT_FREE_TRANSLATED_MESSAGE, cacheKey, modernmtResponse);

        // TODO remove debug
        RedCraftChat.getInstance().getLogger().info("Used " + text.length() + " ModernMT FREE chars to translate to " + targetLangId);

        return modernmtResponse;
    }
}
