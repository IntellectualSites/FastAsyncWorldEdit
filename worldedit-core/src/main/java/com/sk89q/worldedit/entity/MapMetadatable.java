package com.sk89q.worldedit.entity;

import java.util.Map;

public interface MapMetadatable extends Metadatable {

    Map<String, Object> getRawMeta();
    /**
     * Set some session only metadata for the player
     *
     * @param key
     * @param value
     */
    default void setMeta(String key, Object value) {
        getRawMeta().put(key, value);
    }

    default <T> T getAndSetMeta(String key, T value) {
        return (T) getRawMeta().put(key, value);
    }

    default boolean hasMeta() {
        return !getRawMeta().isEmpty();
    }

    default Object putIfAbsent(String key, Object value) {
        return getRawMeta().putIfAbsent(key, value);
    }

    /**
     * Get the metadata for a key.
     *
     * @param <V>
     * @param key
     * @return
     */
    default <V> V getMeta(String key) {
        return (V) getRawMeta().get(key);
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
        V value = (V) getRawMeta().get(key);
        return value == null ? def : value;
    }

    /**
     * Delete the metadata for a key.
     * - metadata is session only
     * - deleting other plugin's metadata may cause issues
     *
     * @param key
     */
    default <V> V deleteMeta(String key) {
        return (V) getRawMeta().remove(key);
    }
}
