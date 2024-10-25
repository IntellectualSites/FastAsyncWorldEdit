package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.bukkit.FaweBukkitWorld;
import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

public class NMSAdapter implements FAWEPlatformAdapterImpl {

    private static final Logger LOGGER = LogManager.getLogger();

    public static int createPalette(
            int[] blockToPalette,
            int[] paletteToBlock,
            int[] blocksCopy,
            char[] set,
            CachedBukkitAdapter adapter,
            short[] nonEmptyBlockCount
    ) {
        short nonAir = 4096;
        int num_palette = 0;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                    ordinal = BlockTypesCache.ReservedIDs.AIR;
                    nonAir--;
                }
                case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR -> nonAir--;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
        }
        int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
        // If bits per entry is over 8, the game uses the global palette.
        if (bitsPerEntry > 8 && adapter != null) {
            // Cannot System#array copy char[] -> int[];
            for (int i = 0; i < adapter.getIbdToStateOrdinal().length; i++) {
                paletteToBlock[i] = adapter.getIbdToStateOrdinal()[i];
            }
            System.arraycopy(adapter.getOrdinalToIbdID(), 0, blockToPalette, 0, adapter.getOrdinalToIbdID().length);
        }
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            int palette = blockToPalette[ordinal];
            blocksCopy[i] = palette;
        }

        if (nonEmptyBlockCount != null) {
            nonEmptyBlockCount[0] = nonAir;
        }
        return num_palette;
    }

    public static int createPalette(
            int layer,
            int[] blockToPalette,
            int[] paletteToBlock,
            int[] blocksCopy,
            Function<Integer, char[]> get,
            char[] set,
            CachedBukkitAdapter adapter,
            short[] nonEmptyBlockCount
    ) {
        short nonAir = 4096;
        int num_palette = 0;
        char[] getArr = null;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                    if (getArr == null) {
                        getArr = get.apply(layer);
                    }
                    // write to set array as this should be a copied array, and will be important when the changes are written
                    // to the GET chunk cached by FAWE. Future dords, actually read this comment please.
                    set[i] = switch (ordinal = getArr[i]) {
                        case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                            nonAir--;
                            yield (ordinal = BlockTypesCache.ReservedIDs.AIR);
                        }
                        case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR,
                                BlockTypesCache.ReservedIDs.VOID_AIR -> {
                            nonAir--;
                            yield ordinal;
                        }
                        default -> ordinal;
                    };
                }
                case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR -> nonAir--;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
        }
        int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
        // If bits per entry is over 8, the game uses the global palette.
        if (bitsPerEntry > 8 && adapter != null) {
            // Cannot System#array copy char[] -> int[];
            for (int i = 0; i < adapter.getIbdToStateOrdinal().length; i++) {
                paletteToBlock[i] = adapter.getIbdToStateOrdinal()[i];
            }
            System.arraycopy(adapter.getOrdinalToIbdID(), 0, blockToPalette, 0, adapter.getOrdinalToIbdID().length);
        }
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                LOGGER.error("Empty (__RESERVED__) ordinal given where not expected, default to air.");
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            int palette = blockToPalette[ordinal];
            blocksCopy[i] = palette;
        }

        if (nonEmptyBlockCount != null) {
            nonEmptyBlockCount[0] = nonAir;
        }
        return num_palette;
    }

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        if (!(chunk instanceof BukkitGetBlocks)) {
            throw new IllegalArgumentException("(IChunkGet) chunk not of type BukkitGetBlocks");
        }
        ((BukkitGetBlocks) chunk).send();
    }

    /**
     * Atomically set the given chunk section to the chunk section array stored in the chunk, given the expected existing chunk
     * section instance at the given layer position.
     * <p>
     * Acquires a (FAWE-implemented only) write-lock on the chunk packet lock, waiting if required before writing, then freeing
     * the lock. Also sets a boolean to indicate a write is waiting and therefore reads should not occur.
     * <p>
     * Utilises ConcurrentHashMap#compute for easy synchronisation for all of the above. Only tryWriteLock is used in blocks
     * synchronised using ConcurrentHashMap methods.
     *
     * @since 2.12.0
     */
    protected static <LevelChunkSection> boolean setSectionAtomic(
            String worldName,
            IntPair pair,
            LevelChunkSection[] sections,
            LevelChunkSection expected,
            LevelChunkSection value,
            int layer
    ) {
        if (layer < 0 || layer >= sections.length) {
            return false;
        }
        StampLockHolder holder = new StampLockHolder();
        ConcurrentHashMap<IntPair, ChunkSendLock> chunks = FaweBukkitWorld.getWorldSendingChunksMap(worldName);
        chunks.compute(pair, (k, lock) -> {
            if (lock == null) {
                lock = new ChunkSendLock();
            } else if (lock.writeWaiting) {
                throw new IllegalStateException("Attempting to write chunk section when write is already ongoing?!");
            }
            holder.stamp = lock.lock.tryWriteLock();
            holder.chunkLock = lock;
            lock.writeWaiting = true;
            return lock;
        });
        try {
            if (holder.stamp == 0) {
                holder.stamp = holder.chunkLock.lock.writeLock();
            }
            return ReflectionUtils.compareAndSet(sections, expected, value, layer);
        } finally {
            chunks = FaweBukkitWorld.getWorldSendingChunksMap(worldName);
            chunks.computeIfPresent(pair, (k, lock) -> {
                if (lock != holder.chunkLock) {
                    throw new IllegalStateException("SENDING_CHUNKS stored lock does not equal lock attempted to be unlocked?!");
                }
                lock.lock.unlockWrite(holder.stamp);
                lock.writeWaiting = false;
                // Keep the lock, etc. in the map as we're going to be accessing again later when sending
                return lock;
            });
        }
    }

    /**
     * Called before sending a chunk packet, filling the given stamp and stampedLock arrays' zeroth indices if the chunk packet
     * send should go ahead.
     * <p>
     * Chunk packets should be sent if both of the following are met:
     *  - There is no more than one current packet send ongoing
     *  - There is no chunk section "write" waiting or ongoing,
     * which are determined by the number of readers currently locking the StampedLock (i.e. the number of sends), if the
     * stamped lock is currently write-locked and if the boolean for waiting write is true.
     * <p>
     * Utilises ConcurrentHashMap#compute for easy synchronisation
     *
     * @since 2.12.0
     */
    protected static void beginChunkPacketSend(String worldName, IntPair pair, StampLockHolder stampedLock) {
        ConcurrentHashMap<IntPair, ChunkSendLock> chunks = FaweBukkitWorld.getWorldSendingChunksMap(worldName);
        chunks.compute(pair, (k, lock) -> {
            if (lock == null) {
                lock = new ChunkSendLock();
            }
            // Allow twice-read-locking, so if the packets have been created but not sent, we can queue another read
            if (lock.writeWaiting || lock.lock.getReadLockCount() > 1 || lock.lock.isWriteLocked()) {
                return lock;
            }
            stampedLock.stamp = lock.lock.readLock();
            stampedLock.chunkLock = lock;
            return lock;
        });
    }

    /**
     * Releases the read lock acquired when sending a chunk packet for a chunk
     *
     * @since 2.12.0
     */
    protected static void endChunkPacketSend(String worldName, IntPair pair, StampLockHolder lockHolder) {
        ConcurrentHashMap<IntPair, ChunkSendLock> chunks = FaweBukkitWorld.getWorldSendingChunksMap(worldName);
        chunks.computeIfPresent(pair, (k, lock) -> {
            if (lock.lock != lockHolder.chunkLock.lock) {
                throw new IllegalStateException("SENDING_CHUNKS stored lock does not equal lock attempted to be unlocked?!");
            }
            lock.lock.unlockRead(lockHolder.stamp);
            // Do not continue to store the lock if we may not need it (i.e. chunk has been sent, may not be sent again)
            return null;
        });
    }

    public static final class StampLockHolder {
        public long stamp;
        public ChunkSendLock chunkLock = null;
    }

    public static final class ChunkSendLock {

        public final StampedLock lock = new StampedLock();
        public boolean writeWaiting = false;

    }

}
