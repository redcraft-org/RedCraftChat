package org.redcraft.redcraftchat.tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.redcraft.redcraftchat.models.translate.TokenizedMessage;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TokenizerManager {

    public static TokenizedMessage tokenizeElements(String originalMessage, boolean tokenizePlayers) {
        // Tokenization is used so important elements of messages don't get translated
        // ! Order is important
        String tokenizedMessage = originalMessage;
        HashMap<String, String> tokenizedElements = new HashMap<String, String>();

        ArrayList<Pattern> patterns = new ArrayList<Pattern>();

        // Tokenize code blocks
        Pattern codeBlockPattern = Pattern.compile("```(.*)```", Pattern.MULTILINE);
        patterns.add(codeBlockPattern);

        // Tokenize code quotes
        Pattern codeQuotePattern = Pattern.compile("`(.*)`");
        patterns.add(codeQuotePattern);

        // Tokenize Discord mentions
        Pattern discordMentionPattern = Pattern.compile("<@(.*)>");
        patterns.add(discordMentionPattern);

        // Tokenize URLs
        Pattern urlPattern = UrlDetection.getUrlPattern();
        patterns.add(urlPattern);

        // Tokenize Minecraft usernames
        if (tokenizePlayers) {
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                Pattern playerUsernamePattern = Pattern.compile(player.getName());
                patterns.add(playerUsernamePattern);
            }
        }

        // Apply patterns
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(tokenizedMessage);

            while (matcher.find()) {
                String match = matcher.group();
                String token = DigestUtils.sha1Hex(match).substring(0, 7);

                tokenizedElements.put(token, match);
                tokenizedMessage = matcher.replaceAll(token);
            }
        }

        // Remove non printable characters
        tokenizedMessage = tokenizedMessage.replaceAll("\\p{C}", "");

        return new TokenizedMessage(tokenizedMessage, tokenizedElements);
    }

    public static String untokenizeElements(TokenizedMessage tokenizedMessage) {
        String message = tokenizedMessage.tokenizedMessage;

        Iterator<Map.Entry<String, String>> tokenizedElementsIterator = tokenizedMessage.tokenizedElements.entrySet().iterator();
        while (tokenizedElementsIterator.hasNext()) {
            Entry<String, String> tokenizedElementsEntry = tokenizedElementsIterator.next();
            message = message.replaceAll(tokenizedElementsEntry.getKey(), tokenizedElementsEntry.getValue());
            tokenizedElementsIterator.remove(); // avoids a ConcurrentModificationException
        }

        return message;
    }
}
