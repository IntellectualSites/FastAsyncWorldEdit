package com.fastasyncworldedit.forge1710.world;

import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Relighter for the M1 write path. Blocks are placed with {@code World.setBlock}, which already updates block and sky
 * light, so there is nothing left to fix. FAWE refuses {@code NullRelighter} for its relight processor, hence this class.
 */
public class Forge1710Relighter implements Relighter {

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
