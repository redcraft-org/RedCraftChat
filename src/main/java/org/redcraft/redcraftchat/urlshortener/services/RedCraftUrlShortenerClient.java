package org.redcraft.redcraftchat.urlshortener.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.models.redcraft_website.RedCraftUrlShortenerRequest;
import org.redcraft.redcraftchat.models.redcraft_website.RedCraftUrlShortenerResponse;

public class RedCraftUrlShortenerClient {

    public static String shorten(String url) throws Exception {
        RedCraftUrlShortenerRequest request = new RedCraftUrlShortenerRequest(url, Config.urlShorteningToken);
        URL endpointUrl = new URL(Config.urlShorteningEndpoint);

        HttpURLConnection httpURLConnection = (HttpURLConnection) endpointUrl.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; utf-8");
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream())) {
            wr.write(request.toString().getBytes());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
        String inputLine;
        StringBuffer rawResponse = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            rawResponse.append(inputLine);
        }
        in.close();

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        RedCraftUrlShortenerResponse response = gson.fromJson(rawResponse.toString(), RedCraftUrlShortenerResponse.class);

        if (!response.response) {
            throw new Exception(String.format("Invalid response for URL shortener: %s", rawResponse));
        }

        return response.shortened;
    }
}
