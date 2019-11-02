package com.sk89q.worldedit.entity;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface MapMetadatable extends Metadatable {

    Map<String, Object> getRawMeta();

    /**
     * {@inheritDoc}
     */
    @Override
    default void setMeta(String key, Object value) {
        getRawMeta().put(key, value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    default <T> T getAndSetMeta(String key, T value) {
        return (T) getRawMeta().put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean hasMeta() {
        return !getRawMeta().isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <V> V getMeta(String key) {
        return (V) getRawMeta().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default <V> V getMeta(String key, @NotNull V def) {
        V value = (V) getRawMeta().get(key);
        return value == null ? def : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <V> V deleteMeta(String key) {
        return (V) getRawMeta().remove(key);
    }
}
