package org.redcraft.redcraftchat.tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vdurmont.emoji.EmojiParser;
import com.vdurmont.emoji.EmojiParser.FitzpatrickAction;

import org.apache.commons.codec.digest.DigestUtils;
import org.redcraft.redcraftchat.models.translate.TokenizedMessage;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TokenizerManager {
    static final Map<String, String> minecraftColorsMappings = new TreeMap<>();

    private TokenizerManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    static {
        // Color codes
        for (int i = 0; i <= 15; i++) {
            String originalCode = String.format("§%01x", i);
            String escapedCode = String.format("§%02d", i);

            minecraftColorsMappings.put(originalCode, escapedCode);
        }

        // bold, italic, etc...
        minecraftColorsMappings.put("§k", "§75");
        minecraftColorsMappings.put("§l", "§76");
        minecraftColorsMappings.put("§m", "§77");
        minecraftColorsMappings.put("§n", "§78");
        minecraftColorsMappings.put("§o", "§79");
        minecraftColorsMappings.put("§r", "§82");
    }

    public static TokenizedMessage tokenizeElements(String originalMessage, boolean tokenizePlayers) {
        // Tokenization is used so important elements of messages don't get translated
        // ! Order is important
        String tokenizedMessage = originalMessage;
        HashMap<String, String> tokenizedElements = new HashMap<>();

        ArrayList<Pattern> patterns = new ArrayList<>();

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

        // Tokenize slash commands
        Pattern slashCommandPattern = Pattern.compile("/([/a-z]+)\\b");
        patterns.add(slashCommandPattern);

        // Tokenize emojis
        tokenizedMessage = EmojiParser.parseToAliases(tokenizedMessage, FitzpatrickAction.PARSE);
        Pattern emojiPattern = Pattern.compile(":((\\w|)*):");
        patterns.add(emojiPattern);

        // Tokenize line returns
        Pattern lineReturnPattern = Pattern.compile("\n", Pattern.MULTILINE);
        patterns.add(lineReturnPattern);

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
                tokenizedMessage = tokenizedMessage.replace(match, token);
            }
        }

        // Tokenize Minecraft color codes
        for (Entry<String, String> mapping : minecraftColorsMappings.entrySet()) {
            tokenizedMessage = tokenizedMessage.replaceAll(mapping.getKey(), mapping.getValue());
        }

        // Remove non printable characters
        tokenizedMessage = tokenizedMessage.replaceAll("\\p{C}", "");

        return new TokenizedMessage(tokenizedMessage, tokenizedElements);
    }

    public static String untokenizeElements(TokenizedMessage tokenizedMessage) {
        String message = tokenizedMessage.getOriginalTokenizedMessage();

        for (Entry<String, String> mapping : minecraftColorsMappings.entrySet()) {
            message = message.replaceAll(mapping.getValue(), mapping.getKey());
        }

        Iterator<Map.Entry<String, String>> tokenizedElementsIterator = tokenizedMessage.getTokenizedElements().entrySet().iterator();
        while (tokenizedElementsIterator.hasNext()) {
            Entry<String, String> tokenizedElementsEntry = tokenizedElementsIterator.next();
            message = message.replaceAll(tokenizedElementsEntry.getKey(), tokenizedElementsEntry.getValue());
            tokenizedElementsIterator.remove(); // avoids a ConcurrentModificationException
        }

        message = EmojiParser.parseToUnicode(message);

        return message;
    }
}
