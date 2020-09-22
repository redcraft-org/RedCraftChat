package org.redcraft.redcraftbungeechat.detection;

import org.redcraft.redcraftbungeechat.detection.services.Lingua;

public class DetectionManager {
    public static String getLanguage(String text) {
        return Lingua.getLanguage(text);
    }
}
