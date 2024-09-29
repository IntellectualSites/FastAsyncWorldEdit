package com.fastasyncworldedit.core.util.task;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class UUIDKeyQueuedThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public UUIDKeyQueuedThreadFactory() {
        group = new ThreadGroup("UUIDKeyQueuedThreadGroup");
        namePrefix = "FAWE UUID-key-queued - ";
    }

    public Thread newThread(Runnable r) {
        Thread t = new UUIDKeyQueuedThread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

    public static final class UUIDKeyQueuedThread extends Thread {

        public UUIDKeyQueuedThread(@Nullable ThreadGroup group, Runnable task, @Nonnull String name, long stackSize) {
            super(group, task, name, stackSize);
        }

    }

}
