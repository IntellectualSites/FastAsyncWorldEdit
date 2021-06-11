package com.fastasyncworldedit.util.task;

public interface Task<T, V> {
    T run(V previousResult);
}
