package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This extent maintains a mapping from Threads to Extents.
 * Whenever an implementation calls {@link #getExtent()}, it will get the extent
 * associated with the current thread, or the one given by the super class if no mapping
 * for this thread exist.
 * <p>
 * There are two ways how to establish a mapping:
 * <ol>
 *     <il>by calling {@link #enter(Extent)}.
 *       This should be called paired with {@link #exit()} to clear the mapping again.</il>
 *     <il>by calling {@link #setCurrentExtent(Extent)}.
 *       This should be called paired with {@link #clearCurrent()}</il>
 * </ol>
 *
 * The first can be used when calling it in the context of a {@link ThreadLocalPassthroughExtent}.
 * The static methods can be called from everywhere, but this requires extra attention to make sure no
 * wrong mapping is kept.
 *
 * @since TODO
 */
@ApiStatus.Internal
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
