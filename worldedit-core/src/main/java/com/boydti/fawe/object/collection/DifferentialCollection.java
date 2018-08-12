package com.boydti.fawe.object.collection;

import com.boydti.fawe.object.change.StreamChange;

public interface DifferentialCollection<T> extends StreamChange {
    public T get();
}
