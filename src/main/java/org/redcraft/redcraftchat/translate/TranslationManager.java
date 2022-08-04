package org.redcraft.redcraftchat.translate;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.models.translate.TokenizedMessage;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;
import org.redcraft.redcraftchat.translate.providers.DeeplProvider;
import org.redcraft.redcraftchat.translate.providers.ModernmtFreeProvider;
import org.redcraft.redcraftchat.translate.providers.ModernmtProvider;
import org.redcraft.redcraftchat.translate.providers.TranslationProvider;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TranslationManager {

    private TranslationProvider translationProvider;

    public TranslationManager(String translationProvider) {
        switch (translationProvider) {
            case "deepl":
                this.translationProvider = new DeeplProvider();
                break;
            case "modernmt-free":
                this.translationProvider = new ModernmtFreeProvider();
                break;
            case "modernmt":
                this.translationProvider = new ModernmtProvider();
                break;
            default:
                throw new IllegalArgumentException("Unknown translation provider: " + translationProvider);
        }
    }

    public String translate(String text, String sourceLanguage, String targetLanguage) throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        if (!Config.translationEnabled) {
            throw new IllegalStateException("TranslationManager was called but translation is disabled in the configuration");
        }

        TokenizedMessage tokenizedMessage = TokenizerManager.tokenizeElements(text, true);

        String translated = this.translationProvider.translate(tokenizedMessage.getOriginalTokenizedMessage(), sourceLanguage.toUpperCase(), targetLanguage.toUpperCase());

        tokenizedMessage.setOriginalTokenizedMessage(translated);

        return TokenizerManager.untokenizeElements(tokenizedMessage);
    }

    // TODO parallelize
    public Map<String, String> translateBulk(String text, String sourceLanguage, List<String> targetLanguages) {
        Map<String, String> translatedLanguages = new HashMap<String, String>();

        for (String targetLanguage : targetLanguages) {
            if (targetLanguage.equalsIgnoreCase(sourceLanguage)) {
                translatedLanguages.put(targetLanguage, text);
                continue;
            }
            try {
                translatedLanguages.put(targetLanguage, this.translate(text, sourceLanguage, targetLanguage));
            } catch (Exception e) {
                translatedLanguages.put(targetLanguage, text);
                e.printStackTrace();
            }
        }

        return translatedLanguages;
    }

    public static String getSourceLanguage(String message, ProxiedPlayer sender) {
        String sourceLanguage = DetectionManager.getLanguage(message);

        if (sourceLanguage == null && sender != null) {
            sourceLanguage = PlayerPreferencesManager.getMainPlayerLanguage(sender);
        }

        return sourceLanguage;
    }

    public static List<String> getTargetLanguages(String sourceLanguage) {
        List<String> targetLanguages = new ArrayList<String>(Config.translationDiscordSupportedLanguages);

        for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
            if (!PlayerPreferencesManager.playerSpeaksLanguage(receiver, sourceLanguage)) {
                String playerLanguage = PlayerPreferencesManager.getMainPlayerLanguage(receiver).toLowerCase();
                if (!targetLanguages.contains(playerLanguage)) {
                    targetLanguages.add(playerLanguage);
                }
            }
        }

        return targetLanguages;
    }

    // Get stuff like EN->FR
    public static String getLanguagePrefix(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null) {
            return null;
        }
        if (targetLanguage == null) {
            return sourceLanguage.toUpperCase();
        }

        String languagePrefix = sourceLanguage.toUpperCase();

        if (!targetLanguage.equalsIgnoreCase(sourceLanguage)) {
            languagePrefix += "➔" + targetLanguage.toUpperCase();
        }

        return languagePrefix;
    }
}
