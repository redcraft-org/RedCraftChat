package org.redcraft.redcraftchat.urlshortener;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.tokenizer.UrlDetection;
import org.redcraft.redcraftchat.tokenizer.UrlTransformer;
import org.redcraft.redcraftchat.urlshortener.services.RedCraftUrlShortenerClient;

public class UrlShortener extends UrlTransformer {
    public static UrlShortener instance = new UrlShortener();

    @Override
    public String transformUrl(String url) {
        try {
            switch (Config.urlShorteningService) {
                case "redcraft_url_shortener":
                    return RedCraftUrlShortenerClient.shorten(url);
                default:
                    throw new Exception(String.format("Unknown URL shortener service \"%s\"", Config.urlShorteningService));
            }
        } catch (Exception ex) {
            RedCraftChat.getInstance().getLogger().severe("Error while shortening URL");
            ex.printStackTrace();
            return url;
        }
    }

    public static String shortenUrls(String text) {
        return UrlDetection.replaceUrls(text, instance);
    }
}
