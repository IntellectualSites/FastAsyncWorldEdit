package com.fastasyncworldedit.core.util.task;

public interface Task<T, V> {
    T run(V previousResult);
}
