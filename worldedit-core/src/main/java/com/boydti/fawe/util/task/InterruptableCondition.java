package com.boydti.fawe.util.task;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;

public class InterruptableCondition {
    private final Condition condition;
    private final Thread thread;
    private final Lock lock;

    public InterruptableCondition(Lock lock, Condition condition, Thread thread) {
        checkNotNull(condition);
        checkNotNull(thread);
        checkNotNull(lock);
        this.lock = lock;
        this.condition = condition;
        this.thread = thread;
    }

    public void signal() {
        try {
            lock.lock();
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void interrupt() {
        this.thread.interrupt();
    }
}
