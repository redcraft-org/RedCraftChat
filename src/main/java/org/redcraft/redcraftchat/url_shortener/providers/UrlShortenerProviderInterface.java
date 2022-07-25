package org.redcraft.redcraftchat.url_shortener.providers;

import java.io.IOException;

interface UrlShortenerProviderInterface {

    public String shorten(String url) throws IllegalStateException, IOException, InterruptedException;
}
