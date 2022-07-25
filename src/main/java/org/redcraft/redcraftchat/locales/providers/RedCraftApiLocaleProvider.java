package org.redcraft.redcraftchat.locales.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.redcraft_api.SupportedLocaleApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class RedCraftApiLocaleProvider extends DatabaseLocaleProvider {

    private HttpClient httpClient = HttpClient.newHttpClient();

    public List<SupportedLocale> getSupportedLocales() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create(Config.supportedLocalesApiUrl))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get supported locales: " + response.statusCode() + " - " + response.body());
        }

        List<SupportedLocaleApi> supportedLocalesList = new Gson().fromJson(response.body(), new TypeToken<List<SupportedLocaleApi>>() {}.getType());

        return transform(supportedLocalesList);
    }

    private List<SupportedLocale> transform(List<SupportedLocaleApi> locale) {
        return locale.stream().map(l -> new SupportedLocale(l.code, l.name)).collect(java.util.stream.Collectors.toList());
    }

}
