package com.fastasyncworldedit.object.collection;

import com.fastasyncworldedit.object.change.StreamChange;

public interface DifferentialCollection<T> extends StreamChange {
    public T get();
}
