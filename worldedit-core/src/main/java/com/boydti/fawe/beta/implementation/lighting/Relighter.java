package com.boydti.fawe.beta.implementation.lighting;

import java.util.concurrent.locks.ReentrantLock;

public interface Relighter extends AutoCloseable {

    /**
     * Add a chunk to be relit when {@link Relighter#removeLighting} etc are called.
     *
     * @param cx chunk x
     * @param cz chunk z
     * @param skipReason byte array of {@link SkipReason} for each chunksection in the chunk. Use case? No idea.
     * @param bitmask Initial bitmask of the chunk (if being editited beforehand)
     * @return Was the chunk added
     */
    boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask);

    /**
     * Add a block to be relit.
     *
     * @param x block x
     * @param y block y
     * @param z block z
     */
    void addLightUpdate(int x, int y, int z);

    /**
     * Safely? Fix block lighting.
     *
     * @param sky whether to also relight sky light values
     */
    void fixLightingSafe(boolean sky);

    /**
     * Remove lighting and then relight safely.
     *
     * @param sky whether to also relight sky light values
     */
    default void removeAndRelight(boolean sky) {
        removeLighting();
        fixLightingSafe(sky);
    }

    /**
     * Clear all chunks and blocks to be relit.
     */
    void clear();

    /**
     * Remove all block and sky light values (set to 0 light) in all chunks added to relighter.
     */
    void removeLighting();

    /**
     * Fix block light values in all chunks added to relighter.
     */
    void fixBlockLighting();

    /**
     * Fix sky light values in all chunks added to relighter.
     */
    void fixSkyLighting();

    /**
     * Are there any block or chunk added to be relit.
     *
     * @return is the relight stuff to be relit empty
     */
    boolean isEmpty();

    ReentrantLock getLock();

    /**
     * Returns true if the Relighter has been flushed
     *
     * @return true if finished
     */
    boolean isFinished();

    class SkipReason {
        public static final byte NONE = 0;
        public static final byte AIR = 1;
        public static final byte SOLID = 2;
    }
}
