package com.fastasyncworldedit.nukkitmot;

import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;

import java.util.concurrent.locks.ReentrantLock;

/**
 * No-op Relighter for Nukkit. Nukkit handles lighting internally,
 * but we need a non-NullRelighter type to satisfy RelightProcessor's constructor check.
 */
public class NukkitRelighter implements Relighter {

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask) {
        return false;
    }

    @Override
    public void addLightUpdate(int x, int y, int z) {
    }

    @Override
    public void fixLightingSafe(boolean sky) {
    }

    @Override
    public void clear() {
    }

    @Override
    public void removeLighting() {
    }

    @Override
    public void fixBlockLighting() {
    }

    @Override
    public void fixSkyLighting() {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void close() {
    }

}
