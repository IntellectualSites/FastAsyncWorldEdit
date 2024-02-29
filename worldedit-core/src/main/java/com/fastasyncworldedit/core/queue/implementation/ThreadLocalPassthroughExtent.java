package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.sk89q.worldedit.extent.Extent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThreadLocalPassthroughExtent extends PassthroughExtent {
    private static final ConcurrentMap<Thread, Extent> extents = new ConcurrentHashMap<>();

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public ThreadLocalPassthroughExtent(final Extent extent) {
        super(extent);
    }

    public static void clearCurrent() {
        extents.remove(Thread.currentThread());
    }

    public static void setCurrentExtent(Extent extent) {
        extents.put(Thread.currentThread(), extent);
    }

    public void enter(Extent extent) {
        extents.put(Thread.currentThread(), extent);
    }

    public void exit() {
        extents.remove(Thread.currentThread());
    }

    @Override
    public Extent getExtent() {
        return extents.getOrDefault(Thread.currentThread(), super.getExtent());
    }

}
