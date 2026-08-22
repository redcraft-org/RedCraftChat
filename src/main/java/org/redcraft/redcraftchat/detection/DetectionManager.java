package org.redcraft.redcraftchat.detection;

import org.redcraft.redcraftchat.detection.services.Lingua;
import org.redcraft.redcraftchat.helpers.LegacyText;

public class DetectionManager {

    private DetectionManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String getLanguage(String text) {
        return Lingua.getLanguage(LegacyText.stripColor(text));
    }
}
