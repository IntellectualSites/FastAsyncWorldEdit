package com.fastasyncworldedit.util.task;

public interface ReceiveTask<T> {
    void run(T previous);
}
