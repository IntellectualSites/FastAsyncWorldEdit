package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.math.LocalBlockVector2Set;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Extent/processor that runs a task the first time a chunk GET is loaded
 *
 * @since 2.13.0
 */
public class OncePerChunkExtent extends AbstractDelegateExtent implements IBatchProcessor {

    private final LocalBlockVector2Set set = new LocalBlockVector2Set();
    private final IQueueExtent<IQueueChunk> queue;
    private Consumer<IChunkGet> task;
    private volatile long lastPair = Long.MAX_VALUE;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param queue  Queue to load chunk GET from if acting as extent not processor
     * @param task   Consumer task for the chunk GET
     * @since 2.13.0
     */
    public OncePerChunkExtent(
            @Nonnull Extent extent,
            @Nonnull IQueueExtent<IQueueChunk> queue,
            @Nonnull Consumer<IChunkGet> task
    ) {
        super(extent);
        this.queue = Objects.requireNonNull(queue);
        this.task = Objects.requireNonNull(task);
    }

    private boolean shouldRun(int chunkX, int chunkZ) {
        synchronized (set) {
            final long pair = (long) chunkX << 32 | chunkZ & 0xffffffffL;
            if (pair == lastPair) {
                return false;
            }
            lastPair = pair;
            return set.add(chunkX, chunkZ);
        }
    }

    private void checkAndRun(int chunkX, int chunkZ) {
        if (shouldRun(chunkX, chunkZ)) {
            task.accept(queue.getCachedGet(chunkX, chunkZ));
        }
    }

    /**
     * Get the task run once per chunk.
     */
    public Consumer<IChunkGet> getTask() {
        return task;
    }

    /**
     * Set the task to be run once per chunk
     */
    public void setTask(Consumer<IChunkGet> task) {
        this.task = task;
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return set;
    }

    @Override
    public IChunkGet processGet(final IChunkGet get) {
        if (shouldRun(get.getX(), get.getZ())) {
            task.accept(get);
        }
        return get;
    }

    @Nullable
    @Override
    public Extent construct(final Extent child) {
        if (getExtent() != child) {
            new ExtentTraverser<Extent>(this).setNext(child);
        }
        return this;
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_BLOCKS;
    }

    @Override
    public BlockState getBlock(final BlockVector3 position) {
        checkAndRun(position.x() >> 4, position.z() >> 4);
        return super.getBlock(position);
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(final BlockVector3 position) {
        checkAndRun(position.x() >> 4, position.z() >> 4);
        return super.getFullBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getBiomeType(x, y, z);
    }

    @Override
    public BiomeType getBiome(final BlockVector3 position) {
        checkAndRun(position.x() >> 4, position.z() >> 4);
        return super.getBiome(position);
    }

    @Override
    public int getEmittedLight(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getEmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getBrightness(x, y, z);
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        checkAndRun(x >> 4, z >> 4);
        return super.setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(final BlockVector3 position, final BiomeType biome) {
        checkAndRun(position.x() >> 4, position.z() >> 4);
        return super.setBiome(position, biome);
    }

    @Override
    public void setBlockLight(final int x, final int y, final int z, final int value) {
        checkAndRun(x >> 4, z >> 4);
        super.setBlockLight(x, y, z, value);
    }

    @Override
    public void setSkyLight(final int x, final int y, final int z, final int value) {
        checkAndRun(x >> 4, z >> 4);
        super.setSkyLight(x, y, z, value);
    }

    /**
     * Reset the chunks visited
     */
    public void reset() {
        lastPair = Long.MAX_VALUE;
        set.clear();
    }

}
