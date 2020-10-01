package org.redcraft.redcraftbungeechat.urlshortener;

import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.tokenizer.UrlTransformer;
import org.redcraft.redcraftbungeechat.urlshortener.services.RedCraftUrlShortenerClient;

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
