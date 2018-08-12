package com.boydti.fawe.util.task;

public interface DelayedTask<T> {
    int getDelay(T previousResult);
}
