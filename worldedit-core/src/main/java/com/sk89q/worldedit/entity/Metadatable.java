package com.sk89q.worldedit.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Metadatable {

    /**
     * Set some session only metadata for the player
     *
     * @param key
     * @param value
     */
    void setMeta(String key, Object value);

    <T> T getAndSetMeta(String key, T value);

    /**
     * Checks if the object contains any metadata.
     *
     * @return {@code true} if there is metadata set for the object
     */
    boolean hasMeta();

    /**
     * Gets the metadata value to which the specified key is mapped,
     * or {@code null} if the key is not set.
     *
     * @param key the key of the metadata value to retrieve
     * @return the value of the metadata or {@code null} if none exists
     */
    <V> V getMeta(String key);

    /**
     * Gets the metadata value to which the specified key is mapped,
     * or the default value if no metadata exists for the key.
     *
     * @param key the key of the metadata value to retrieve
     * @param defaultValue the value to return if there is no metadata for the given key
     * @return the metadata value for the key, if present; else the default value
     */
    @NotNull
    default <V> V getMeta(String key, @NotNull V defaultValue) {
        V value = getMeta(key);
        return value == null ? defaultValue : value;    }

    /**
     * Deletes the given metadata key from object. Do not delete metadata set by another plugin
     * unless you know what you are doing.
     *
     * @param key the key identifying the metadata to remove.
     * @return the previous value associated with they given key
     */
    @Nullable
    <V> V deleteMeta(String key);

    final class METADATA_KEYS {

        public static final String ANVIL_CLIPBOARD = "anvil-clipboard";
        public static final String ROLLBACK = "rollback";
    }
}
