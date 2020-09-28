package org.redcraft.redcraftbungeechat.translate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.models.deepl.DeeplResponse;
import org.redcraft.redcraftbungeechat.models.translate.TokenizedMessage;
import org.redcraft.redcraftbungeechat.translate.services.DeeplClient;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TranslationManager {
    public static String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        if (!Config.translationEnabled) {
            throw new Exception("TranslationManager was called but translation is disabled in the configuration");
        }

        switch (Config.translationService) {
            case "deepl":
                TokenizedMessage tokenizedMessage = tokenizeElements(text, true);
                DeeplResponse dr = DeeplClient.translate(tokenizedMessage.tokenizedMessage, sourceLanguage, targetLanguage);
                String translated = DeeplClient.parseDeeplResponse(dr);
                tokenizedMessage.tokenizedMessage = translated;
                return untokenizeElements(tokenizedMessage);
            default:
                throw new Exception("Unknown translation service \"" + Config.translationService + "\"");
        }
    }

    public static TokenizedMessage tokenizeElements(String originalMessage, boolean tokenizePlayers) {
        // Tokenization is used so important elements of messages don't get translated
        // ! Order is important
        String tokenizedMessage = originalMessage;
        HashMap<String, String> tokenizedElements = new HashMap<String, String>();

        ArrayList<Pattern> patterns = new ArrayList<Pattern>();

        // Tokenize code blocks
        Pattern codeBlockPattern = Pattern.compile("```(.*)```");
        patterns.add(codeBlockPattern);

        // Tokenize code quotes
        Pattern codeQuotePattern = Pattern.compile("`(.*)`");
        patterns.add(codeQuotePattern);

        // Tokenize Discord mentions
        Pattern discordMentionPattern = Pattern.compile("<@(.*)>");
        patterns.add(discordMentionPattern);

        // Tokenize URLs
        // Regex from BungeeCord
        // https://github.com/SpigotMC/BungeeCord/blob/master/chat/src/main/java/net/md_5/bungee/api/chat/TextComponent.java
        Pattern urlPattern = Pattern.compile("(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?");
        patterns.add(urlPattern);

        // Tokenize Minecraft usernames
        if (tokenizePlayers) {
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                Pattern playerUsernamePattern = Pattern.compile(player.getName());
                patterns.add(playerUsernamePattern);
            }
        }

        // Tokenize emojis
        // TODO

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(tokenizedMessage);

            while (matcher.find()) {
                String match = matcher.group();
                String token = DigestUtils.sha1Hex(match).substring(0, 7); // TODO replace with uuid

                tokenizedElements.put(token, match);
                tokenizedMessage = matcher.replaceAll(token);
            }
        }

        return new TokenizedMessage(tokenizedMessage, tokenizedElements);
    }

    public static String untokenizeElements(TokenizedMessage tokenizedMessage) {
        String message = tokenizedMessage.tokenizedMessage;

        Iterator<Map.Entry<String, String>> it = tokenizedMessage.tokenizedElements.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> pair = it.next();
            message = message.replaceAll(pair.getKey(), pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
        return message;
    }
}
