package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.processor.EmptyBatchProcessor;
import com.fastasyncworldedit.core.extent.processor.MultiBatchProcessor;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

public interface IBatchProcessor {

    /**
     * Process a chunk that has been set.
     */
    IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set);

    /**
     * Post-process a chunk that has been edited. Set should NOT be modified here, changes will NOT be flushed to the world,
     * but MAY be flushed to history. Defaults to nothing as most Processors will not use it. Post-processors that are not
     * technically blocking should override this method to allow post-processors to become blocking if required.
     */
    default Future<?> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        // Do not need to default to below method. FAWE itself by default will only call the method below.
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Post-process a chunk that has been edited. Set should NOT be modified here, changes will NOT be flushed to the world,
     * but MAY be flushed to history. Defaults to nothing as most Processors will not use it. If the post-processor will run
     * tasks asynchronously/not be blocking, use {@link IBatchProcessor#postProcessSet} to return a Future.
     *
     * @since 2.1.0
     */
    default void postProcess(IChunk chunk, IChunkGet get, IChunkSet set) {
        // Default to above for compatibility and to ensure whatever method is overridden by child classes is called
        postProcessSet(chunk, get, set);
    }

    default boolean processGet(int chunkX, int chunkZ) {
        return true;
    }

    /**
     * Convert this processor into an Extent based processor instead of a queue batch based on.
     */
    @Nullable
    Extent construct(Extent child);

    /**
     * Utility method to trim a chunk based on min and max Y (inclusive).
     *
     * @param keepInsideRange if all blocks inside the range (inclusive) should be kept (default), or removed
     * @return false if chunk is empty of blocks
     */
    default boolean trimY(IChunkSet set, int minY, int maxY, final boolean keepInsideRange) {
        int minLayer = minY >> 4;
        int maxLayer = maxY >> 4;
        if (keepInsideRange) {
            for (int layer = set.getMinSectionPosition(); layer <= minLayer; layer++) {
                if (set.hasSection(layer)) {
                    if (layer == minLayer) {
                        char[] arr = set.loadIfPresent(layer);
                        if (arr != null) {
                            int index = (minY & 15) << 8;
                            for (int i = 0; i < index; i++) {
                                arr[i] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                            set.setBlocks(layer, arr);
                        }
                    } else {
                        set.setBlocks(layer, null);
                    }
                }
            }
            for (int layer = maxLayer; layer <= set.getMaxSectionPosition(); layer++) {
                if (set.hasSection(layer)) {
                    if (layer == maxLayer) {
                        char[] arr = set.loadIfPresent(layer);
                        if (arr != null) {
                            int index = ((maxY + 1) & 15) << 8;
                            for (int i = index; i < arr.length; i++) {
                                arr[i] = BlockTypesCache.ReservedIDs.__RESERVED__;
                            }
                            set.setBlocks(layer, arr);
                        }
                    } else {
                        set.setBlocks(layer, null);
                    }
                }
            }
            try {
                int layer = (minY - 15) >> 4;
                while (layer < (maxY + 15) >> 4) {
                    if (set.hasSection(layer)) {
                        return true;
                    }
                    layer++;
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                WorldEdit.logger.error("IBatchProcessor: minY = {} , layer = {}", minY, ((minY - 15) >> 4), exception);
            }
            return false;
        }
        int chunkMaxY = (set.getMaxSectionPosition() << 4) + 15;
        int chunkMinY = set.getMinSectionPosition() << 4;
        if (maxY >= chunkMaxY && minY <= chunkMinY) {
            set.reset();
            return false;
        }
        boolean hasBlocks = false;
        for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
            if (layer < minLayer || layer > maxLayer) {
                hasBlocks |= set.hasSection(layer);
                continue;
            }
            if (layer == minLayer) {
                char[] arr = set.loadIfPresent(layer);
                if (arr != null) {
                    int index = (minY & 15) << 8;
                    for (int i = index; i < 4096; i++) {
                        arr[i] = BlockTypesCache.ReservedIDs.__RESERVED__;
                    }
                }
                set.setBlocks(layer, arr);
            } else if (layer == maxLayer) {
                char[] arr = set.loadIfPresent(layer);
                if (arr != null) {
                    int index = ((maxY + 1) & 15) << 8;
                    for (int i = 0; i < index; i++) {
                        arr[i] = BlockTypesCache.ReservedIDs.__RESERVED__;
                    }
                }
                set.setBlocks(layer, arr);
            } else {
                set.setBlocks(layer, null);
            }
        }
        return hasBlocks;
    }

    /**
     * Utility method to trim entity and blocks with a provided contains function.
     *
     * @return false if chunk is empty of NBT
     * @deprecated tiles are stored in chunk-normalised coordinate space and thus cannot use the same function as entities
     */
    @Deprecated(forRemoval = true, since = "2.8.4")
    default boolean trimNBT(IChunkSet set, Function<BlockVector3, Boolean> contains) {
        Set<CompoundTag> ents = set.getEntities();
        if (!ents.isEmpty()) {
            ents.removeIf(ent -> !contains.apply(ent.getEntityPosition().toBlockPoint()));
        }
        Map<BlockVector3, CompoundTag> tiles = set.getTiles();
        if (!tiles.isEmpty()) {
            tiles.entrySet().removeIf(blockVector3CompoundTagEntry -> !contains
                    .apply(blockVector3CompoundTagEntry.getKey()));
        }
        return !tiles.isEmpty() || !ents.isEmpty();
    }

    /**
     * Utility method to trim entity and blocks with a provided contains function.
     *
     * @return false if chunk is empty of NBT
     * @since 2.8.4
     */
    default boolean trimNBT(
            IChunkSet set, Function<BlockVector3, Boolean> containsEntity, Function<BlockVector3, Boolean> containsTile
    ) {
        Set<CompoundTag> ents = set.getEntities();
        if (!ents.isEmpty()) {
            ents.removeIf(ent -> !containsEntity.apply(ent.getEntityPosition().toBlockPoint()));
        }
        Map<BlockVector3, CompoundTag> tiles = set.getTiles();
        if (!tiles.isEmpty()) {
            tiles.entrySet().removeIf(blockVector3CompoundTagEntry -> !containsTile.apply(blockVector3CompoundTagEntry.getKey()));
        }
        return !tiles.isEmpty() || !ents.isEmpty();
    }

    /**
     * Join two processors and return the result.
     */
    default IBatchProcessor join(IBatchProcessor other) {
        return MultiBatchProcessor.of(this, other);
    }

    default IBatchProcessor joinPost(IBatchProcessor other) {
        return MultiBatchProcessor.of(this, other);
    }

    default void flush() {
    }

    /**
     * Return a new processor after removing all are instances of a specified class.
     */
    default <T extends IBatchProcessor> IBatchProcessor remove(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            return EmptyBatchProcessor.getInstance();
        }
        return this;
    }

    /**
     * Default to CUSTOM ProcessorScope as we want custom processors people add to be before we write history, but after FAWE does it's own stuff.
     */
    default ProcessorScope getScope() {
        return ProcessorScope.CUSTOM;
    }

}
