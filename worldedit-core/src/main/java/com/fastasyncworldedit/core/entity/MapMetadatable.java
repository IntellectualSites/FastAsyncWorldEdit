package com.fastasyncworldedit.core.entity;

import javax.annotation.Nonnull;
import java.util.Map;

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
    @SuppressWarnings("unchecked")
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

    default Object putIfAbsent(String key, Object value) {
        return getRawMeta().putIfAbsent(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    default <V> V getMeta(String key) {
        return (V) getRawMeta().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    default <V> V getMeta(String key, @Nonnull V def) {
        V value = (V) getRawMeta().get(key);
        return value == null ? def : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    default <V> V deleteMeta(String key) {
        return (V) getRawMeta().remove(key);
    }

}
