package com.boydti.fawe.object.task;

import com.boydti.fawe.util.TaskManager;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleAsyncNotifyQueue extends AsyncNotifyQueue {
    private ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private Thread.UncaughtExceptionHandler handler;

    public SimpleAsyncNotifyQueue(Thread.UncaughtExceptionHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean hasQueued() {
        return !tasks.isEmpty();
    }

    @Override
    public void operate() {
        while (!tasks.isEmpty()) {
            Runnable task = tasks.poll();
            try {
                if (task != null) task.run();
            } catch (Throwable e) {
                if (handler != null) handler.uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    public void queue(Runnable queueTask) {
        synchronized (lock) {
            if (queueTask != null) tasks.add(queueTask);
            if (!running.get()) {
                running.set(true);
                TaskManager.IMP.async(task);
            }
        }
    }

    public int getSize() {
        return tasks.size();
    }

    public void clear() {
        tasks.clear();
    }
}
