package org.redcraft.redcraftchat.caching.providers;

import java.util.List;

import org.redcraft.redcraftchat.Config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisCache implements CacheProvider {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;

    public RedisCache() {
        redisClient = RedisClient.create("redis://localhost:6379/0");
        redisConnection = redisClient.connect();
    }

    public boolean putRaw(String key, String serializedObject) {
        redisConnection.sync().set(getKeyName(key), serializedObject);
        return true;
    }

    public String getRaw(String key) {
        return redisConnection.sync().get(getKeyName(key));
    }

    public boolean delete(String key) {
        if (!exists(key)) {
            return false;
        }

        redisConnection.sync().del(getKeyName(key));
        return true;
    }

    public boolean flush() {
        List<String> keysToDelete = redisConnection.sync().keys(getKeyName("*"));
        redisConnection.sync().del(keysToDelete.toArray(new String[keysToDelete.size()]));
        return true;
    }

    public boolean exists(String key) {
        return redisConnection.sync().exists(getKeyName(key)) > 0;
    }

    public String getKeyName(String keyName) {
        return String.format("%s;%s", Config.redisKeyPrefix, keyName);
    }
}
