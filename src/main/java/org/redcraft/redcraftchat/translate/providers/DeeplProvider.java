package org.redcraft.redcraftchat.translate.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.deepl.DeeplResponse;
import org.redcraft.redcraftchat.models.deepl.DeeplTranslation;
import org.redcraft.redcraftchat.models.deepl.DeeplSupportedLanguage;

public class DeeplProvider {

    private static HashMap<String, DeeplSupportedLanguage> supportedLanguages = new HashMap<>();
    private static boolean supportedLanguagesInitialized = false;

    private DeeplProvider() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static DeeplResponse translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException {
        String cacheKey = String.format("%s;%s;%s", sourceLanguageId, targetLanguageId, text);

        DeeplResponse cachedDeeplResponse = (DeeplResponse) CacheManager.get(CacheCategory.DEEPL_TRANSLATED_MESSAGE, cacheKey, DeeplResponse.class);

        if (cachedDeeplResponse != null) {
            return cachedDeeplResponse;
        }

        DeeplSupportedLanguage sourceLang = DeeplProvider.getLanguage(sourceLanguageId);
        DeeplSupportedLanguage targetLang = DeeplProvider.getLanguage(targetLanguageId);

        if (sourceLang == null) {
            throw new IllegalStateException("The source language " + sourceLanguageId + " is not supported by Deepl");
        }

        if (targetLang == null) {
            throw new IllegalStateException("The source language " + targetLanguageId + " is not supported by Deepl");
        }

        URIBuilder ub = new URIBuilder(Config.deeplEndpoint);
        ub.addParameter("auth_key", Config.deeplToken);
        ub.addParameter("text", text);
        ub.addParameter("source_lang", sourceLang.languageId);
        ub.addParameter("target_lang", targetLang.languageId);
        ub.addParameter("preserve_formatting", Config.deeplPreserveFormatting ? "1" : "0");

        if (targetLang.formalityAvailable) {
            ub.addParameter("formality", Config.deeplFormality);
        }

        URL endpointUrl = new URL(ub.toString());

        HttpURLConnection httpURLConnection = (HttpURLConnection) endpointUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setDoOutput(true);

        // TODO remove debug
        RedCraftChat.getInstance().getLogger().info("Used " + text.length() + " Deepl chars to translate to " + targetLanguageId);

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

        DeeplResponse deeplResponse = gson.fromJson(rawResponse.toString(), DeeplResponse.class);

        CacheManager.put(CacheCategory.DEEPL_TRANSLATED_MESSAGE, cacheKey, deeplResponse);

        return deeplResponse;
    }

    public static String parseDeeplResponse(DeeplResponse dr) {
        ArrayList<String> translations = new ArrayList<String>();
        for (DeeplTranslation translation : dr.translations) {
            translations.add(translation.text);
        }

        return String.join(" ", translations);
    }

    public static DeeplSupportedLanguage getLanguage(String id) throws IllegalStateException {
        if (!supportedLanguagesInitialized) {
            supportedLanguagesInitialized = true;

            // TODO put this in database
            supportedLanguages.put("EN", new DeeplSupportedLanguage("EN", false));
            supportedLanguages.put("FR", new DeeplSupportedLanguage("FR", true));
            supportedLanguages.put("DE", new DeeplSupportedLanguage("DE", true));
            supportedLanguages.put("IT", new DeeplSupportedLanguage("IT", true));
            supportedLanguages.put("JA", new DeeplSupportedLanguage("JA", false));
            supportedLanguages.put("ES", new DeeplSupportedLanguage("ES", false));
            supportedLanguages.put("NL", new DeeplSupportedLanguage("NL", true));
            supportedLanguages.put("PL", new DeeplSupportedLanguage("PL", true));
            supportedLanguages.put("PT", new DeeplSupportedLanguage("PT", true));
            supportedLanguages.put("RU", new DeeplSupportedLanguage("RU", true));
            supportedLanguages.put("ZH", new DeeplSupportedLanguage("ZH", false));
        }

        try {
            return supportedLanguages.get(id.toUpperCase());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not find language %s in the supported languages: " + id, ex.fillInStackTrace());
        }
    }
}
