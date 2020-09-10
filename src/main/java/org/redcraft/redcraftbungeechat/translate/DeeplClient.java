package org.redcraft.redcraftbungeechat.translate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.models.DeeplResponse;
import org.redcraft.redcraftbungeechat.models.DeeplTranslation;

public class DeeplClient {

    public static DeeplResponse translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        URIBuilder ub = new URIBuilder("https://api.deepl.com/v2/translate");
        ub.addParameter("auth_key", Config.deeplToken);
        ub.addParameter("text", text);
        ub.addParameter("source_lang", sourceLanguage);
        ub.addParameter("target_lang", targetLanguage);
        // TODO check for formality availability and get it from config
        if (sourceLanguage.equals("EN")) {
            ub.addParameter("formality", "less");
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
}
