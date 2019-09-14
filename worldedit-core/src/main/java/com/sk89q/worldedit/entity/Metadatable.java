package com.sk89q.worldedit.entity;

public interface Metadatable {

    /**
     * Set some session only metadata for the player
     *
     * @param key
     * @param value
     * @return previous value
     */
    void setMeta(String key, Object value);

    <T> T getAndSetMeta(String key, T value);

    boolean hasMeta();

    /**
     * Get the metadata for a key.
     *
     * @param <V>
     * @param key
     * @return
     */
    <V> V getMeta(String key);

    /**
     * Get the metadata for a specific key (or return the default provided)
     *
     * @param key
     * @param def
     * @param <V>
     * @return
     */
    default <V> V getMeta(String key, V def) {
        V value = (V) getMeta(key);
        return value == null ? def : value;    }

    /**
     * Delete the metadata for a key.
     * - metadata is session only
     * - deleting other plugin's metadata may cause issues
     *
     * @param key
     */
    <V> V deleteMeta(String key);

    final class METADATA_KEYS {

        public static final String ANVIL_CLIPBOARD = "anvil-clipboard";
        public static final String ROLLBACK = "rollback";
    }
}
