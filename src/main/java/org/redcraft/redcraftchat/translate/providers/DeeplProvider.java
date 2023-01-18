package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;

import org.apache.http.client.utils.URIBuilder;
import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.deepl.DeeplResponse;
import org.redcraft.redcraftchat.models.deepl.DeeplTranslation;
import org.redcraft.redcraftchat.models.deepl.DeeplSupportedLanguage;

public class DeeplProvider implements TranslationProvider {

    private HttpClient httpClient;
    private HashMap<String, DeeplSupportedLanguage> supportedLanguages;

    public DeeplProvider() {
        httpClient = HttpClient.newHttpClient();

        supportedLanguages = new HashMap<>();
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

    public String translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        String sourceLangId = sourceLanguageId.toLowerCase().split("-")[0];
        String targetLangId = targetLanguageId.toLowerCase().split("-")[0];
        String cacheKey = String.format("%s;%s;%s", sourceLangId, targetLangId, text);

        String cachedDeeplResponse = (String) CacheManager.get(CacheCategory.DEEPL_TRANSLATED_MESSAGE, cacheKey, String.class);

        if (cachedDeeplResponse != null) {
            return cachedDeeplResponse;
        }

        DeeplSupportedLanguage sourceLang = getLanguage(sourceLangId);
        DeeplSupportedLanguage targetLang = getLanguage(targetLangId);

        if (sourceLang == null) {
            throw new IllegalStateException("The source language " + sourceLangId + " is not supported by Deepl");
        }

        if (targetLang == null) {
            throw new IllegalStateException("The source language " + sourceLangId + " is not supported by Deepl");
        }

        URIBuilder url = new URIBuilder(Config.deeplEndpoint);
        url.addParameter("auth_key", Config.deeplToken);
        url.addParameter("text", text);
        url.addParameter("source_lang", sourceLang.languageId);
        url.addParameter("target_lang", targetLang.languageId);
        url.addParameter("preserve_formatting", Config.deeplPreserveFormatting ? "1" : "0");

        if (targetLang.formalityAvailable) {
            url.addParameter("formality", Config.deeplFormality);
        }

        HttpRequest request = HttpRequest.newBuilder(url.build())
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Autorization", "DeepL-Auth-Key " + Config.deeplToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        DeeplResponse deeplResponse = new Gson().fromJson(response.body(), DeeplResponse.class);

        // TODO remove debug
        String debugMessage = "Used " + text.length() + " Deepl chars to translate to " + targetLangId;
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        ArrayList<String> translations = new ArrayList<String>();
        for (DeeplTranslation translation : deeplResponse.translations) {
            translations.add(translation.text);
        }

        String translated = String.join(" ", translations);

        CacheManager.put(CacheCategory.DEEPL_TRANSLATED_MESSAGE, cacheKey, translated);

        return translated;
    }

    private DeeplSupportedLanguage getLanguage(String id) throws IllegalStateException {
        try {
            return supportedLanguages.get(id.toUpperCase());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not find language %s in the supported languages: " + id, ex.fillInStackTrace());
        }
    }
}
