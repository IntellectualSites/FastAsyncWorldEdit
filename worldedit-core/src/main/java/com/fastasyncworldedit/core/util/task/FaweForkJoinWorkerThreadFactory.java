package com.fastasyncworldedit.core.util.task;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Internal
public class FaweForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    private final String nameFormat;
    private final AtomicInteger idCounter;

    public FaweForkJoinWorkerThreadFactory(String nameFormat) {
        this.nameFormat = nameFormat;
        this.idCounter = new AtomicInteger(0);
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        final ForkJoinWorkerThread worker = new FaweForkJoinThread(pool);
        worker.setName(String.format(nameFormat, idCounter.getAndIncrement()));
        return worker;
    }

}
