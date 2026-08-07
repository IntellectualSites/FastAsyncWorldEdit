package com.fastasyncworldedit.nukkit;

import cn.nukkit.level.Level;
import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Relighter for Nukkit that recalculates chunk light after bulk FAWE edits.
 * <p>
 * FAWE edits chunks via raw {@code FullChunk.setFullBlockId}, which bypasses the per-block
 * lighting propagation that the vanilla {@code Level.setBlock} path performs. Without an
 * explicit recalculation the chunk's stored {@code skyLight}/{@code blockLight} arrays stay
 * stale and are sent verbatim to Bedrock clients (whose chunk packets carry server-computed
 * light), producing dark patches and glowing artifacts.
 * <p>
 * This relighter records the chunk coordinates touched by an edit ({@link #addChunk}) and, on
 * {@link #fixLightingSafe} / {@link #fixBlockLighting} / {@link #fixSkyLighting}, delegates to
 * {@link NukkitImplAdapter#recalculateLight(Object)} for each registered chunk. The fork-specific
 * adapter selects the correct recalculation surface ({@code recalculateHeightMap},
 * {@code populateSkyLight}, {@code populateBlockLight} as available).
 * <p>
 * A non-null {@code Level} reference is captured at construction; if the world has been unloaded
 * the relighter becomes a safe no-op rather than throwing.
 *
 * @see com.fastasyncworldedit.core.extent.processor.lighting.RelightProcessor
 * @see NukkitImplAdapter#recalculateLight(Object)
 */
public class NukkitRelighter implements Relighter {

    private final ReentrantLock lock = new ReentrantLock();
    private final Level level;
    private final Set<Long> chunks = new HashSet<>();

    public NukkitRelighter(Level level) {
        this.level = level;
    }

    private static long key(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    @Override
    public boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask) {
        if (level == null) {
            return false;
        }
        chunks.add(key(cx, cz));
        return true;
    }

    @Override
    public void addLightUpdate(int x, int y, int z) {
        // Granular single-block updates are handled by the full chunk recalc below.
    }

    @Override
    public void fixLightingSafe(boolean sky) {
        relight();
    }

    @Override
    public void clear() {
        chunks.clear();
    }

    @Override
    public void removeLighting() {
        // No separate lighting tear-down: recalculation overwrites stale arrays in place.
    }

    @Override
    public void fixBlockLighting() {
        relight();
    }

    @Override
    public void fixSkyLighting() {
        relight();
    }

    private void relight() {
        if (level == null || chunks.isEmpty()) {
            return;
        }
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        for (long key : chunks) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            Object chunk = adapter.getChunk(level, cx, cz);
            if (chunk != null) {
                adapter.recalculateLight(chunk);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return chunks.isEmpty();
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
        chunks.clear();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(level);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NukkitRelighter)) {
            return false;
        }
        NukkitRelighter that = (NukkitRelighter) other;
        return Objects.equals(level, that.level);
    }

}
