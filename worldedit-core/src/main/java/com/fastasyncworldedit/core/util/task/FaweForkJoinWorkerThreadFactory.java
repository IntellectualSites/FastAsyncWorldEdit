package com.fastasyncworldedit.core.util.task;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class FaweForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    private final String nameFormat;

    public FaweForkJoinWorkerThreadFactory(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        worker.setName(String.format(nameFormat, worker.getPoolIndex()));
        return worker;
    }

}
