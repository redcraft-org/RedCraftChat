package org.redcraft.redcraftchat.caching.providers;

import java.util.HashMap;
import java.util.Map;

public class MemoryCache implements CacheProvider {
    private Map<String, String> cache;

    public MemoryCache() {
        cache = new HashMap<>();
    }

    public boolean putRaw(String key, String serializedObject) {
        cache.put(key, serializedObject);
        return true;
    }

    public String getRaw(String key) {
        if (exists(key)) {
            return cache.get(key);
        }

        return null;
    }

    public boolean delete(String key) {
        if (!exists(key)) {
            return false;
        }

        cache.remove(key);
        return true;
    }

    public boolean flush() {
        cache.clear();
        return true;
    }

    public boolean exists(String key) {
        return cache.containsKey(key);
    }
}
