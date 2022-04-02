package org.redcraft.redcraftchat.translate;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.database.PlayerPreferencesManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.models.deepl.DeeplResponse;
import org.redcraft.redcraftchat.models.modernmt.ModernmtResponse;
import org.redcraft.redcraftchat.models.translate.TokenizedMessage;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;
import org.redcraft.redcraftchat.translate.services.DeeplClient;
import org.redcraft.redcraftchat.translate.services.ModernmtClient;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TranslationManager {

    String translationService;

    public TranslationManager(String translationService) {
        this.translationService = translationService;
    }

    public String translate(String text, String sourceLanguage, String targetLanguage) throws IllegalStateException, URISyntaxException, IOException {
        if (!Config.translationEnabled) {
            throw new IllegalStateException("TranslationManager was called but translation is disabled in the configuration");
        }

        TokenizedMessage tokenizedMessage = TokenizerManager.tokenizeElements(text, true);

        switch (this.translationService) {
            case "deepl":
                DeeplResponse dr = DeeplClient.translate(tokenizedMessage.getOriginalTokenizedMessage(), sourceLanguage.toUpperCase(), targetLanguage.toUpperCase());
                tokenizedMessage.setOriginalTokenizedMessage(DeeplClient.parseDeeplResponse(dr));
                break;
            case "modernmt":
                ModernmtResponse mr = ModernmtClient.translate(tokenizedMessage.getOriginalTokenizedMessage(), sourceLanguage.toLowerCase(), targetLanguage.toLowerCase());
                tokenizedMessage.setOriginalTokenizedMessage(mr.data.translation.replaceAll("§( )+", "§")); // Fix for MC message
                break;
            default:
                throw new IllegalStateException(String.format("Unknown translation service \"%s\"", this.translationService));
        }

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

        if (sourceLanguage == null) {
            sourceLanguage = PlayerPreferencesManager.getMainPlayerLanguage(sender);
        }

        return sourceLanguage;
    }

    public static List<String> getTargetLanguages(String sourceLanguage) {
        List<String> targetLanguages = new ArrayList<String>(Config.translationSupportedLanguages);

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
        String languagePrefix = sourceLanguage.toUpperCase();

        if (!targetLanguage.equalsIgnoreCase(sourceLanguage)) {
            languagePrefix += "➔" + targetLanguage.toUpperCase();
        }

        return languagePrefix;
    }
}
