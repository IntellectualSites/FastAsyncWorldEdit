package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.fastasyncworldedit.core.util.collection.CleanableThreadLocal;
import com.sk89q.worldedit.extent.Extent;

public class ThreadLocalPassthroughExtent extends PassthroughExtent {

    private final CleanableThreadLocal<Extent> localExtent;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public ThreadLocalPassthroughExtent(final Extent extent) {
        super(extent);
        // fallback to one extent
        this.localExtent = new CleanableThreadLocal<>(() -> extent);
    }

    public void enter(Extent extent) {
        this.localExtent.set(extent);
    }

    public void exit() {
        this.localExtent.remove();
    }

    @Override
    public Extent getExtent() {
        return this.localExtent.get();
    }

}
