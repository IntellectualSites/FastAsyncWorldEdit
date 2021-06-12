package com.fastasyncworldedit.core.util.task;

public interface DelayedTask<T> {
    int getDelay(T previousResult);
}
