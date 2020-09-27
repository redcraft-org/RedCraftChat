package org.redcraft.redcraftbungeechat.caching;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.models.caching.CacheCategory;

public class CacheManager {
    public static Object get(String key, Class<?> classType) {
        String stringifiedObject;

        if (Config.redisEnabled) {
            stringifiedObject = RedisCache.getRaw(key);
        } else {
            stringifiedObject = MemoryCache.getRaw(key);
        }

        return deserializeObject(stringifiedObject, classType);
    }

    public static boolean put(String key, Object element) {
        String serializedObject = serializeObject(element);
        if (Config.redisEnabled) {
            return RedisCache.putRaw(key, serializedObject);
        } else {
            return MemoryCache.putRaw(key, serializedObject);
        }
    }

    public static boolean delete(String key) {
        if (Config.redisEnabled) {
            return RedisCache.delete(key);
        } else {
            return MemoryCache.delete(key);
        }
    }

    public static boolean flush() {
        if (Config.redisEnabled) {
            return RedisCache.flush();
        } else {
            return MemoryCache.flush();
        }
    }

    public static boolean exists(String key) {
        if (Config.redisEnabled) {
            return RedisCache.exists(key);
        } else {
            return MemoryCache.exists(key);
        }
    }

    private static String serializeObject(Object element) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(element);
    }

    private static Object deserializeObject(String element, Class<?> classType) {
        return new Gson().fromJson(element, classType);
    }

    public static Object get(CacheCategory category, String key, Class<?> classType) {
        return get(formatCategoryKey(category, key), classType);
    }

    public static boolean put(CacheCategory category, String key, Object element) {
        return put(formatCategoryKey(category, key), element);
    }

    public static String formatCategoryKey(CacheCategory category, String key) {
        return String.format("%s;%s", category, key);
    }
}
