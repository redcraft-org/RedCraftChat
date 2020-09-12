package org.redcraft.redcraftbungeechat.translate.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.models.deepl.DeeplResponse;
import org.redcraft.redcraftbungeechat.models.deepl.DeeplTranslation;
import org.redcraft.redcraftbungeechat.models.deepl.DeeplSupportedLanguage;

public class DeeplClient {

    private static HashMap<String, DeeplSupportedLanguage> supportedLanguages = new HashMap<String, DeeplSupportedLanguage>();
    private static boolean supportedLanguagesInitialized = false;

    public static DeeplResponse translate(String text, String sourceLanguageId, String targetLanguageId) throws Exception {
        DeeplSupportedLanguage sourceLang = DeeplClient.getLanguage(sourceLanguageId);
        DeeplSupportedLanguage targetLang = DeeplClient.getLanguage(targetLanguageId);

        URIBuilder ub = new URIBuilder(Config.deeplEndpoint);
        ub.addParameter("auth_key", Config.deeplToken);
        ub.addParameter("text", text);
        ub.addParameter("source_lang", sourceLang.languageId);
        ub.addParameter("target_lang", targetLang.languageId);
        ub.addParameter("preserve_formatting", Config.deeplPreserveFormatting ? "1" : "0");

        if (targetLang.formalityAvailable) {
            ub.addParameter("formality", Config.deeplFormality);
        }

        URL url = new URL(ub.toString());

        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setDoOutput(true);

        BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        return gson.fromJson(response.toString(), DeeplResponse.class);
    }

    public static String parseDeeplResponse(DeeplResponse dr) {
        ArrayList<String> translations = new ArrayList<String>();
        for (DeeplTranslation translation : dr.translations) {
            translations.add(translation.text);
        }

        return String.join(" ", translations);
    }

    public static DeeplSupportedLanguage getLanguage(String id) throws Exception {
        if (!supportedLanguagesInitialized) {
            supportedLanguagesInitialized = true;

            supportedLanguages.put("EN", new DeeplSupportedLanguage("EN-US", false));
            supportedLanguages.put("FR", new DeeplSupportedLanguage("FR", true));
            supportedLanguages.put("DE", new DeeplSupportedLanguage("DE", true));
            supportedLanguages.put("IT", new DeeplSupportedLanguage("IT", true));
            supportedLanguages.put("JA", new DeeplSupportedLanguage("JA", false));
            supportedLanguages.put("ES", new DeeplSupportedLanguage("ES", false));
            supportedLanguages.put("NL", new DeeplSupportedLanguage("NL", true));
            supportedLanguages.put("PL", new DeeplSupportedLanguage("PL", true));
            supportedLanguages.put("PT", new DeeplSupportedLanguage("PT", true));
            supportedLanguages.put("BR", new DeeplSupportedLanguage("PT-BR", true));
            supportedLanguages.put("RU", new DeeplSupportedLanguage("RU", true));
            supportedLanguages.put("ZH", new DeeplSupportedLanguage("ZH", false));
        }

        try {
            return supportedLanguages.get(id.toUpperCase());
        } catch (Exception ex) {
            throw new Exception("Could not find language " + id + " in the supported languages", ex.fillInStackTrace());
        }
    }
}
