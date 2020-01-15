package com.boydti.fawe.beta.implementation.queue;

public interface Pool<T> {
    T poll();
    default boolean offer(T recycle) {
        return false;
    }
    default void clear() {}
}

