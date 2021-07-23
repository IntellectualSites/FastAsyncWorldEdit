package com.fastasyncworldedit.core.queue;

@FunctionalInterface
public interface Pool<T> {
    T poll();
    default boolean offer(T recycle) {
        return false;
    }
    default void clear() {}
}

