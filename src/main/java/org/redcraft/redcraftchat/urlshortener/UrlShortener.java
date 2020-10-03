package org.redcraft.redcraftchat.urlshortener;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.tokenizer.UrlTransformer;
import org.redcraft.redcraftchat.urlshortener.services.RedCraftUrlShortenerClient;

public class UrlShortener extends UrlTransformer {
    @Override
    public String transformUrl(String url) {
        // This does not replace URLs because
        // you're meant to extend this class
        // and override your replacement method
        return url;
    }

    public String shortenUrl(String url) throws Exception {
        switch (Config.urlShorteningService) {
            case "redcraft_url_shortener":
                return RedCraftUrlShortenerClient.shorten(url);
            default:
                throw new Exception(String.format("Unknown URL shortener service \"%s\"", Config.urlShorteningService));
        }
    }
}
