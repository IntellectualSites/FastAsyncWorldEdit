package com.boydti.fawe.util.task;

public interface ReceiveTask<T> {
    void run(T previous);
}
