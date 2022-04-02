package org.redcraft.redcraftchat.detection;

import org.redcraft.redcraftchat.detection.services.Lingua;

public class DetectionManager {

    private DetectionManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String getLanguage(String text) {
        return Lingua.getLanguage(text);
    }
}
