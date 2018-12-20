package com.boydti.fawe.object;

import java.util.concurrent.ConcurrentHashMap;

public class Metadatable {

    private final ConcurrentHashMap<String, Object> meta = new ConcurrentHashMap<>();

    /**
     * Set some session only metadata for the player
     *
     * @param key
     * @param value
     * @return previous value
     */
    public void setMeta(String key, Object value) {
        this.meta.put(key, value);
    }

    public <T> T getAndSetMeta(String key, T value) {
        return (T) this.meta.put(key, value);
    }

    public boolean hasMeta() {
        return !meta.isEmpty();
    }

    /**
     * Get the metadata for a key.
     *
     * @param <V>
     * @param key
     * @return
     */
    public <V> V getMeta(String key) {
        if (this.meta != null) {
            return (V) this.meta.get(key);
        }
        return null;
    }

    /**
     * Get the metadata for a specific key (or return the default provided)
     *
     * @param key
     * @param def
     * @param <V>
     * @return
     */
    public <V> V getMeta(String key, V def) {
        if (this.meta != null) {
            V value = (V) this.meta.get(key);
            return value == null ? def : value;
        }
        return def;
    }

    /**
     * Delete the metadata for a key.
     * - metadata is session only
     * - deleting other plugin's metadata may cause issues
     *
     * @param key
     */
    public <V> V deleteMeta(String key) {
        return this.meta == null ? null : (V) this.meta.remove(key);
    }
}
