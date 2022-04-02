package org.redcraft.redcraftchat.detection.services;

import java.util.ArrayList;

import com.github.pemistahl.lingua.api.*;
import com.github.pemistahl.lingua.api.Language;

import org.redcraft.redcraftchat.Config;

public class Lingua {

    private static LanguageDetector detector = null;

    private Lingua() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String getLanguage(String text) {
        if (detector == null) {
            ArrayList<Language> supportedLanguages = new ArrayList<Language>();

            for (String supportedLanguage : Config.translationSupportedLanguages) {
                IsoCode639_1 supportedLanguageIsoCode = IsoCode639_1.valueOf(supportedLanguage.toUpperCase());
                supportedLanguages.add(Language.getByIsoCode639_1(supportedLanguageIsoCode));
            }

            Language[] argsList = supportedLanguages.toArray(new Language[supportedLanguages.size()]);

            // TODO move that relative distant to config
            detector = LanguageDetectorBuilder.fromLanguages(argsList).withMinimumRelativeDistance(0.1).build();
        }

        Language language = detector.detectLanguageOf(text);

        if (language.equals(Language.UNKNOWN)) {
            return null;
        }

        return language.getIsoCode639_1().toString();
    }
}
