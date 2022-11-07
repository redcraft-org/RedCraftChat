package org.redcraft.redcraftchat.url_shortener.providers;

import java.io.IOException;

public interface UrlShortenerProvider {

    public String shorten(String url) throws IllegalStateException, IOException, InterruptedException;
}
