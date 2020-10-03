package org.redcraft.redcraftchat.detection.services;

import java.util.ArrayList;

import com.github.pemistahl.lingua.api.*;
import com.github.pemistahl.lingua.api.Language;

import org.redcraft.redcraftchat.Config;

public class Lingua {

    private static LanguageDetector detector = null;

    public static String getLanguage(String text) {
        if (detector == null) {
            ArrayList<Language> supportedLanguages = new ArrayList<Language>();

            for (String supportedLanguage : Config.translationSupportedLanguages) {
                IsoCode639_1 supportedLanguageIsoCode = IsoCode639_1.valueOf(supportedLanguage.toUpperCase());
                supportedLanguages.add(Language.getByIsoCode639_1(supportedLanguageIsoCode));
            }

            Language[] argsList = supportedLanguages.toArray(new Language[supportedLanguages.size()]);

            detector = LanguageDetectorBuilder.fromLanguages(argsList).build();
        }
        return detector.detectLanguageOf(text).getIsoCode639_1().toString();
    }
}
