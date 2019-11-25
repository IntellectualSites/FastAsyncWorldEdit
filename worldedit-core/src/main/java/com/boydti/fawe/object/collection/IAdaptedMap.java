package com.boydti.fawe.object.collection;

import com.google.common.base.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface IAdaptedMap<K, V, K2, V2> extends Map<K, V> {
    Map<K2, V2> getParent();

    K2 adaptKey(K key);

    V2 adaptValue(V value);

    K adaptKey2(K2 key);

    V adaptValue2(V2 value);

    @Override
    default int size() {
        return getParent().size();
    }

    @Override
    default boolean isEmpty() {
        return getParent().isEmpty();
    }

    @Override
    default boolean containsKey(Object key) {
        return getParent().containsKey(adaptKey((K) key));
    }

    @Override
    default boolean containsValue(Object value) {
        return getParent().containsValue(adaptValue((V) value));
    }

    @Override
    default V get(Object key) {
        return adaptValue2(getParent().get(adaptKey((K) key)));
    }

    @Nullable
    @Override
    default V put(K key, V value) {
        return adaptValue2(getParent().put(adaptKey(key), adaptValue(value)));
    }

    @Override
    default V remove(Object key) {
        return adaptValue2(getParent().remove(adaptKey((K) key)));
    }

    @Override
    default void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    default void clear() {
        getParent().clear();
    }

    @NotNull
    @Override
    default Set<K> keySet() {
        if (isEmpty()) return Collections.emptySet();
        return new AdaptedSetCollection<>(getParent().keySet(), this::adaptKey2);
    }

    @NotNull
    @Override
    default Collection<V> values() {
        if (isEmpty()) return Collections.emptySet();
        return new AdaptedSetCollection<>(getParent().values(), this::adaptValue2);
    }

    @NotNull
    @Override
    default Set<Entry<K, V>> entrySet() {
        if (isEmpty()) return Collections.emptySet();
        return new AdaptedSetCollection<>(getParent().entrySet(), new Function<Entry<K2, V2>, Entry<K, V>>() {
            private MutablePair<K, V> entry = new MutablePair<>();
            @Override
            public Entry<K, V> apply(@javax.annotation.Nullable Entry<K2, V2> input) {
                entry.setKey(adaptKey2(input.getKey()));
                entry.setValue(adaptValue2(input.getValue()));
                return entry;
            }
        });
    }
}
