package com.boydti.fawe.object.collection;

import com.google.common.base.Functions;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class AdaptedMap<K, V, K2, V2> implements IAdaptedMap<K, V, K2, V2> {
    private final Map<K2, V2> parent;
    private final Function<V2, V> value2;
    private final Function<K2, K> key2;
    private final Function<V, V2> value;
    private final Function<K, K2> key;

    private static final Function SAME = Functions.identity();

    private static final Function IMMUTABLE = o -> { throw new UnsupportedOperationException("Immutable"); };

    public static <K, K2, V> AdaptedMap<K, V, K2, V> keys(Map<K2, V> parent, Function<K, K2> key, Function<K2, K> key2) {
        return new AdaptedMap<K, V, K2, V>(parent, key, key2, SAME, SAME);
    }

    public static <K, V, V2> AdaptedMap<K, V, K, V2> values(Map<K, V2> parent, Function<V, V2> value, Function<V2, V> value2) {
        return new AdaptedMap<K, V, K, V2>(parent, SAME, SAME, value, value2);
    }

    public static <K, K2, V, V2> AdaptedMap<K, V, K2, V2> immutable(Map<K2, V2> parent, Function<K2, K> key, Function<V2, V> value) {
        return new AdaptedMap<K, V, K2, V2>(parent, IMMUTABLE, key, IMMUTABLE, value);
    }

    public AdaptedMap(Map<K2, V2> parent, Function<K, K2> key, Function<K2, K> key2, Function<V, V2> value, Function<V2, V> value2) {
        this.parent = parent;
        this.key = key;
        this.value = value;
        this.key2 = key2;
        this.value2 = value2;
    }

    @Override
    public Map<K2, V2> getParent() {
        return this.parent;
    }

    @Override
    public K2 adaptKey(K key) {
        return this.key.apply(key);
    }

    @Override
    public V2 adaptValue(V value) {
        return this.value.apply(value);
    }

    @Override
    public K adaptKey2(K2 key) {
        return this.key2.apply(key);
    }

    @Override
    public V adaptValue2(V2 value) {
        return value2.apply(value);
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        if (isEmpty()) return Collections.emptySet();
        return new AdaptedSetCollection<>(getParent().entrySet(), new com.google.common.base.Function<Entry<K2, V2>, Entry<K, V>>() {
            private AdaptedPair entry = new AdaptedPair();
            @Override
            public Entry<K, V> apply(@javax.annotation.Nullable Entry<K2, V2> input) {
                entry.input = input;
                return entry;
            }
        });
    }

    public class AdaptedPair implements Entry<K, V> {
        private Entry<K2, V2> input;

        @Override
        public K getKey() {
            return adaptKey2(input.getKey());
        }

        @Override
        public V getValue() {
            return adaptValue2(input.getValue());
        }

        @Override
        public V setValue(V value) {
            return adaptValue2(input.setValue(adaptValue(value)));
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Entry) {
                return Objects.equals(((Entry) o).getKey(), getKey()) && Objects.equals(((Entry) o).getValue(), getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1337;
        }
    }
}
