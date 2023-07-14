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

import java.util.function.Consumer;

/**
 * Extent/processor that runs a t
 */
public class OncePerChunkExtent extends AbstractDelegateExtent implements IBatchProcessor {

    private final LocalBlockVector2Set set = new LocalBlockVector2Set();
    private final IQueueExtent<IQueueChunk> queue;
    private final Consumer<IChunkGet> task;
    private volatile long lastPair = Long.MAX_VALUE;
    private volatile boolean isProcessing;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public OncePerChunkExtent(Extent extent, IQueueExtent<IQueueChunk> queue, Consumer<IChunkGet> task) {
        super(extent);
        this.queue = queue;
        this.task = task;
    }

    private boolean shouldRun(int chunkX, int chunkZ) {
        final long pair = (long) chunkX << 32 | chunkZ & 0xffffffffL;
        if (pair == lastPair) {
            return false;
        }
        lastPair = pair;
        synchronized (set) {
            if (!set.contains(chunkX, chunkZ)) {
                set.add(chunkX, chunkZ);
                return true;
            }
        }
        return false;
    }

    private void checkAndRun(int chunkX, int chunkZ) {
        if (!isProcessing && shouldRun(chunkX, chunkZ)) {
            task.accept(queue.getCachedGet(chunkX, chunkZ));
        }
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return set;
    }

    @Override
    public IChunkGet processGet(final IChunkGet get) {
        isProcessing = true;
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
        checkAndRun(position.getBlockX() >> 4, position.getBlockZ() >> 4);
        return super.getBlock(position);
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        checkAndRun(x >> 4, z >> 4);
        return super.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(final BlockVector3 position) {
        checkAndRun(position.getBlockX() >> 4, position.getBlockZ() >> 4);
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
        checkAndRun(position.getBlockX() >> 4, position.getBlockZ() >> 4);
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
        checkAndRun(position.getBlockX() >> 4, position.getBlockZ() >> 4);
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

}
