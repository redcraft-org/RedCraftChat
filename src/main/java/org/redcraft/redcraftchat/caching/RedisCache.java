package org.redcraft.redcraftchat.caching;

import java.util.List;

import org.redcraft.redcraftchat.Config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisCache {
    private static RedisClient redisClient = null;
    private static StatefulRedisConnection<String, String> redisConnection = null;

    private RedisCache() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static boolean connect() {
        redisClient = RedisClient.create("redis://localhost:6379/0");
        redisConnection = redisClient.connect();

        return redisConnection.isOpen();
    }

    public static void disconnect() {
        if (redisClient != null) {
            if (redisConnection != null && redisConnection.isOpen()) {
                redisConnection.close();
            }
            redisClient.shutdown();
        }
    }

    public static boolean connectIfDisconnected() {
        if (redisConnection == null || !redisConnection.isOpen()) {
            return connect();
        }
        return true;
    }

    public static boolean putRaw(String key, String serializedObject) {
        if (!connectIfDisconnected()) {
            return false;
        }

        redisConnection.sync().set(getKeyName(key), serializedObject);
        return true;
    }

    public static String getRaw(String key) {
        if (!connectIfDisconnected()) {
            return null;
        }

        return redisConnection.sync().get(getKeyName(key));
    }

    public static boolean delete(String key) {
        if (!connectIfDisconnected()) {
            return false;
        }

        if (!exists(key)) {
            return false;
        }

        redisConnection.sync().del(getKeyName(key));
        return true;
    }

    public static boolean flush() {
        if (!connectIfDisconnected()) {
            return false;
        }

        List<String> keysToDelete = redisConnection.sync().keys(getKeyName("*"));
        redisConnection.sync().del(keysToDelete.toArray(new String[keysToDelete.size()]));
        return true;
    }

    public static boolean exists(String key) {
        if (!connectIfDisconnected()) {
            return false;
        }

        return redisConnection.sync().exists(getKeyName(key)) > 0;
    }

    public static String getKeyName(String keyName) {
        return String.format("%s;%s", Config.redisKeyPrefix, keyName);
    }
}
