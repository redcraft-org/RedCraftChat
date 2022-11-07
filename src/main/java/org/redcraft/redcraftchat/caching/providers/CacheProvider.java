package org.redcraft.redcraftchat.caching.providers;

public interface CacheProvider {
    public boolean putRaw(String key, String serializedObject);
    public String getRaw(String key);
    public boolean delete(String key);
    public boolean flush();
    public boolean exists(String key);
}
