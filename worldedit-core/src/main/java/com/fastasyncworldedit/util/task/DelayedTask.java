package com.fastasyncworldedit.util.task;

public interface DelayedTask<T> {
    int getDelay(T previousResult);
}
