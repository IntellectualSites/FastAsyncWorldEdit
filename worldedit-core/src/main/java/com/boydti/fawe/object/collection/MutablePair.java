package com.boydti.fawe.object.collection;

import java.util.Map;

public class MutablePair<K, V> implements Map.Entry<K, V> {
    private K key;
    private V value;
    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = value;
        this.value = value;
        return old;
    }

    public void setKey(K key) {
        this.key = key;
    }
}
