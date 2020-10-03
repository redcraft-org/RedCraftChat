package org.redcraft.redcraftchat.detection;

import org.redcraft.redcraftchat.detection.services.Lingua;

public class DetectionManager {
    public static String getLanguage(String text) {
        return Lingua.getLanguage(text);
    }
}
