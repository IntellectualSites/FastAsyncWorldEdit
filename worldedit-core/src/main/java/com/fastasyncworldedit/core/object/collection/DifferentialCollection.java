package com.fastasyncworldedit.core.object.collection;

import com.fastasyncworldedit.core.object.change.StreamChange;

public interface DifferentialCollection<T> extends StreamChange {
    public T get();
}
