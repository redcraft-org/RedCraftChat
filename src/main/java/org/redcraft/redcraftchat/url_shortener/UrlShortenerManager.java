package org.redcraft.redcraftchat.url_shortener;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.tokenizer.UrlDetection;
import org.redcraft.redcraftchat.tokenizer.UrlTransformer;
import org.redcraft.redcraftchat.url_shortener.providers.RedCraftApiUrlShortenerProvider;

public class UrlShortenerManager extends UrlTransformer {

    private static RedCraftApiUrlShortenerProvider shorteningProvider;

    private static UrlShortenerManager instance = new UrlShortenerManager();

    public static RedCraftApiUrlShortenerProvider getShorteningProvider() {
        if (shorteningProvider == null) {
            switch (Config.urlShorteningProvider) {
                case "redcraft":
                    shorteningProvider = new RedCraftApiUrlShortenerProvider();
                    break;

                default:
                    throw new IllegalStateException("Unknown database player Provider: " + Config.urlShorteningProvider);
            }
        }
        return shorteningProvider;
    }

    @Override
    public String transformUrl(String url) {
        try {
            if (Config.urlShorteningProvider.equals("redcraft")) {
                return getShorteningProvider().shorten(url);
            } else {
                throw new IllegalStateException(String.format("Unknown URL shortener service \"%s\"", Config.urlShorteningProvider));
            }
        } catch (Exception ex) {
            RedCraftChat.getInstance().getLogger().severe("Error while shortening URL");
            ex.printStackTrace();
        }
        return url;
    }

    public static String shortenUrls(String text) {
        return UrlDetection.replaceUrls(text, instance);
    }

    public static UrlShortenerManager getInstance() {
        return instance;
    }
}
