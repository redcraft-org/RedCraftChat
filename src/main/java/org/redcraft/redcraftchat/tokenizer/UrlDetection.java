package org.redcraft.redcraftchat.tokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlDetection {

    private UrlDetection() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String replaceUrls(String text, UrlTransformer urlTransformer) {
        Pattern pattern = getUrlPattern();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String originalUrl = matcher.group();
            String urlReplacement = urlTransformer.transformUrl(originalUrl);
            text = matcher.replaceAll(urlReplacement);
        }

        return text;
    }

    public static Pattern getUrlPattern() {
        // Regex from BungeeCord
        // https://github.com/SpigotMC/BungeeCord/blob/master/chat/src/main/java/net/md_5/bungee/api/chat/TextComponent.java
        return Pattern.compile("(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?");
    }
}
