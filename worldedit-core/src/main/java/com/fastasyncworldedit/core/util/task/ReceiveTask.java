package com.fastasyncworldedit.core.util.task;

public interface ReceiveTask<T> {
    void run(T previous);
}
