package com.fastasyncworldedit.nukkit;

import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;

import java.util.concurrent.locks.ReentrantLock;

/**
 * No-op relighter for Nukkit. Nukkit handles lighting internally,
 * but a non-null Relighter implementation is required to satisfy
 * RelightProcessor's constructor check.
 * <p>
 * On Bukkit, FAWE must explicitly recalculate block and sky light after
 * bulk edits because the vanilla server does not reliably update lighting
 * for NMS-level chunk modifications. Nukkit, being a Bedrock server
 * implementation, updates lighting automatically when blocks change
 * through its public API. There is no exposed API for manual relighting.
 * <p>
 * All methods in this class are intentionally no-op. Using NullRelighter
 * would cause RelightProcessor to skip lighting entirely, which could
 * break downstream code that expects a valid Relighter instance.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit requires active relighting via NMS; Nukkit does not</li>
 *   <li>All relight methods are no-op; lighting is automatic</li>
 *   <li>Must implement Relighter interface rather than returning null</li>
 * </ul>
 *
 * @see com.fastasyncworldedit.core.extent.processor.lighting.RelightProcessor
 * @see com.fastasyncworldedit.core.extent.processor.lighting.Relighter
 */
public class NukkitRelighter implements Relighter {

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @implNote No-op: Nukkit handles lighting updates internally when chunks or blocks change.
     */
    @Override
    public boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask) {
        return false;
    }

    /**
     * @implNote No-op: Nukkit handles lighting updates internally when blocks change.
     */
    @Override
    public void addLightUpdate(int x, int y, int z) {
    }

    /**
     * @implNote No-op: Nukkit handles lighting updates internally and does not expose manual relighting.
     */
    @Override
    public void fixLightingSafe(boolean sky) {
    }

    /**
     * @implNote No-op: Nukkit handles lighting updates internally and does not expose manual relighting.
     */
    @Override
    public void clear() {
    }

    /**
     * @implNote No-op: Nukkit handles lighting updates internally and does not expose manual relighting.
     */
    @Override
    public void removeLighting() {
    }

    /**
     * @implNote No-op: Nukkit handles lighting updates internally and does not expose manual relighting.
     */
    @Override
    public void fixBlockLighting() {
    }

    /**
     * @implNote No-op: Nukkit handles lighting updates internally and does not expose manual relighting.
     */
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

    /**
     * @implNote No-op: Nukkit handles lighting updates internally and does not expose manual relighting.
     */
    @Override
    public void close() {
    }

}
