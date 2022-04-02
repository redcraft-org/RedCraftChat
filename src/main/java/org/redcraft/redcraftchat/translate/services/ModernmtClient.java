package org.redcraft.redcraftchat.translate.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.modernmt.ModernmtResponse;

public class ModernmtClient {

    private ModernmtClient() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static ModernmtResponse translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException {
        String cacheKey = String.format("%s;%s;%s", sourceLanguageId, targetLanguageId, text);

        ModernmtResponse cachedModernmtResponse = (ModernmtResponse) CacheManager.get(CacheCategory.MODERNMT_TRANSLATED_MESSAGE, cacheKey, ModernmtResponse.class);

        if (cachedModernmtResponse != null) {
            return cachedModernmtResponse;
        }

        if (targetLanguageId == null) {
            throw new IllegalStateException("The source language " + targetLanguageId + " is not supported by Modernmt");
        }

        URIBuilder ub = new URIBuilder("https://webapi.modernmt.com/translate");
        ub.addParameter("q", text);
        ub.addParameter("source", sourceLanguageId);
        ub.addParameter("target", targetLanguageId);

        URL endpointUrl = new URL(ub.toString());

        HttpURLConnection httpURLConnection = (HttpURLConnection) endpointUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setDoOutput(true);

        // TODO remove debug
        RedCraftChat.getInstance().getLogger().info("Used " + text.length() + " Modernmt chars to translate to " + targetLanguageId);

        BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
        String inputLine;
        StringBuilder rawResponse = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            rawResponse.append(inputLine);
        }
        in.close();

        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        ModernmtResponse modernmtResponse = gson.fromJson(rawResponse.toString(), ModernmtResponse.class);

        CacheManager.put(CacheCategory.MODERNMT_TRANSLATED_MESSAGE, cacheKey, modernmtResponse);

        return modernmtResponse;
    }
}
