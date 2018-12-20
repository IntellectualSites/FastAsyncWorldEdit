package com.boydti.fawe.util.task;

public interface Task<T, V> {
    T run(V previousResult);
}
