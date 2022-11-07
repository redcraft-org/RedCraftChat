package org.redcraft.redcraftchat.url_shortener.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.models.redcraft_api.RedCraftUrlShortenerRequest;
import org.redcraft.redcraftchat.models.redcraft_api.RedCraftUrlShortenerResponse;

public class RedCraftApiUrlShortenerProvider implements UrlShortenerProvider {

    private HttpClient httpClient = HttpClient.newHttpClient();

    public String shorten(String url) throws IllegalStateException, IOException, InterruptedException {
        String body = new Gson().toJson(new RedCraftUrlShortenerRequest(url));

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(Config.urlShorteningEndpoint))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            return null;
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get player preferences: " + response.statusCode() + " - " + response.body());
        }

        RedCraftUrlShortenerResponse shortenedUrl = new Gson().fromJson(response.body(), RedCraftUrlShortenerResponse.class);

        if (!shortenedUrl.response) {
            throw new IllegalStateException(String.format("Invalid response for URL shortener: %s", response.body()));
        }

        return shortenedUrl.shortened;
    }
}
