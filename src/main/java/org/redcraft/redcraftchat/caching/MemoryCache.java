package org.redcraft.redcraftchat.caching;

import java.util.HashMap;

public class MemoryCache {
    private static HashMap<String, String> cache = new HashMap<String, String>();

    public static boolean putRaw(String key, String serializedObject) {
        cache.put(key, serializedObject);
        return true;
    }

    public static String getRaw(String key) {
        if (exists(key)) {
            return cache.get(key);
        }

        return null;
    }

    public static boolean delete(String key) {
        if (!exists(key)) {
            return false;
        }

        cache.remove(key);
        return true;
    }

    public static boolean flush() {
        cache = new HashMap<String, String>();
        return true;
    }

    public static boolean exists(String key) {
        return cache.containsKey(key);
    }
}
