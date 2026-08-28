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
import org.redcraft.redcraftchat.translate.providers.ClaudeProvider;
import org.redcraft.redcraftchat.translate.providers.DeeplProvider;
import org.redcraft.redcraftchat.translate.providers.ModernmtFreeProvider;
import org.redcraft.redcraftchat.translate.providers.ModernmtProvider;
import org.redcraft.redcraftchat.translate.providers.TranslationProvider;

import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;

public class TranslationManager {

    private TranslationProvider translationProvider;

    public TranslationManager(String translationProvider) {
        switch (translationProvider) {
            case "claude":
                this.translationProvider = new ClaudeProvider();
                break;
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

        // Numbers come out before anything else, so a message that only
        // differs by a counter is one cache entry instead of one translation
        // per tick. See NumericTemplate for the case that prompted it.
        NumericTemplate numbers = NumericTemplate.of(text);
        if (numbers.isTemplated() && !numbers.hasWordsLeft()) {
            // Nothing but the numbers, so there is nothing to translate
            return text;
        }

        String toTranslate = numbers.template();
        String translated = translateThroughProvider(toTranslate, sourceLanguage, targetLanguage);

        String restored = numbers.restore(translated);
        // A translation that dropped a placeholder cannot be put back
        // together, and showing the original beats showing a stray
        // %number_b% to a player
        return restored != null ? restored : text;
    }

    private String translateThroughProvider(String text, String sourceLanguage, String targetLanguage)
            throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        if (this.translationProvider.translatesRawText()) {
            // Nothing is hidden from the provider, it was told what the codes
            // and placeholders mean instead
            return this.translationProvider.translate(text, sourceLanguage.toUpperCase(), targetLanguage.toUpperCase());
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

    public static String getSourceLanguage(String message, Player sender) {
        String sourceLanguage = DetectionManager.getLanguage(message);

        if (sourceLanguage == null && sender != null) {
            sourceLanguage = PlayerPreferencesManager.getMainPlayerLanguage(sender);
        }

        return sourceLanguage;
    }

    public static List<String> getTargetLanguages(String sourceLanguage) {
        List<String> targetLanguages = new ArrayList<String>(Config.translationDiscordSupportedLanguages);

        for (Player receiver : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
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
