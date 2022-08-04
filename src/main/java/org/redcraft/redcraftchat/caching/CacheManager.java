package org.redcraft.redcraftchat.caching;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.caching.converters.LocaleDateTimeConverter;
import org.redcraft.redcraftchat.caching.providers.CacheProvider;
import org.redcraft.redcraftchat.caching.providers.MemoryCache;
import org.redcraft.redcraftchat.caching.providers.RedisCache;
import org.redcraft.redcraftchat.models.caching.CacheCategory;

public class CacheManager {

    private CacheManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static CacheProvider getCacheProvider() {
        switch (Config.cacheProvider) {
            case "memory":
                return new MemoryCache();
            case "redis":
                return new RedisCache();

            default:
                throw new IllegalStateException("Unknown cache provider: " + Config.cacheProvider);
        }
    }

    public static Object get(String key, Class<?> classType) {
        return deserializeObject(getCacheProvider().getRaw(key), classType);
    }

    public static Object get(String key, Type classType) {
        return deserializeObject(getCacheProvider().getRaw(key), classType);
    }

    public static boolean put(String key, Object element) {
        return getCacheProvider().putRaw(key, serializeObject(element));
    }

    public static boolean delete(String key) {
        return getCacheProvider().delete(key);
    }

    public static boolean flush() {
        return getCacheProvider().flush();
    }

    public static boolean exists(String key) {
        return getCacheProvider().exists(key);
    }

    public static Object get(CacheCategory category, String key, Class<?> classType) {
        return get(formatCategoryKey(category, key), classType);
    }

    public static Object get(CacheCategory category, String key, Type classType) {
        return get(formatCategoryKey(category, key), classType);
    }

    public static boolean put(CacheCategory category, String key, Object element) {
        return put(formatCategoryKey(category, key), element);
    }

    public static Object delete(CacheCategory category, String key) {
        return delete(formatCategoryKey(category, key));
    }

    public static String formatCategoryKey(CacheCategory category, String key) {
        return String.format("%s;%s", category, key);
    }

    private static String serializeObject(Object element) {
        return getGson().toJson(element);
    }

    private static Object deserializeObject(String element, Class<?> classType) {
        return getGson().fromJson(element, classType);
    }

    private static Object deserializeObject(String element, Type classType) {
        return getGson().fromJson(element, classType);
    }

    private static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocaleDateTimeConverter());
        return builder.create();
    }
}
