package com.sk89q.worldedit.entity.metadata;

import java.util.Map;

public interface Metadatable {
    Map<String, Object> getMetaMap();

    /**
     * Set some session only metadata for the player
     *
     * @param key
     * @param value
     * @return previous value
     */
    default void setMeta(String key, Object value) {
        getMetaMap().put(key, value);
    }

    default <T> T getAndSetMeta(String key, T value) {
        return (T) getMetaMap().put(key, value);
    }

    default boolean hasMeta() {
        return !getMetaMap().isEmpty();
    }

    /**
     * Get the metadata for a key.
     *
     * @param <V>
     * @param key
     * @return
     */
    default <V> V getMeta(String key) {
        if (getMetaMap() != null) {
            return (V) getMetaMap().get(key);
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
    default <V> V getMeta(String key, V def) {
        if (getMetaMap() != null) {
            V value = (V) getMetaMap().get(key);
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
    default <V> V deleteMeta(String key) {
        return getMetaMap() == null ? null : (V) getMetaMap().remove(key);
    }
}
