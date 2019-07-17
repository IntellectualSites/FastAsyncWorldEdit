package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.ChunkFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.filters.CountFilter;
import com.boydti.fawe.beta.filters.DistrFilter;
import com.boydti.fawe.config.Settings;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;

public class MultiThreadedQueue extends AbstractDelegateExtent implements IQueueWrapper {
    private final World world;
    private final QueueHandler handler;

    protected MultiThreadedQueue(QueueHandler handler, World world) {
        super(handler.getQueue(world));
        this.world = world;
        this.handler = handler;
    }

    public IQueueExtent getQueue() {
        return handler.getQueue(this.world);
    }

    public <T extends Filter> T apply(final Region region, final T filter) {
        // The chunks positions to iterate over
        final Set<BlockVector2> chunks = region.getChunks();
        final Iterator<BlockVector2> chunksIter = chunks.iterator();

        // Get a pool, to operate on the chunks in parallel
        final int size = Math.min(chunks.size(), Settings.IMP.QUEUE.PARALLEL_THREADS);
        final ForkJoinTask[] tasks = new ForkJoinTask[size];
        for (int i = 0; i < size; i++) {
            tasks[i] = handler.submit(new Runnable() {
                @Override
                public void run() {
                    final Filter newFilter = filter.fork();
                    // Create a chunk that we will reuse/reset for each operation
                    final IQueueExtent queue = wrapQueue(getQueue());
                    synchronized (queue) {
                        ChunkFilterBlock block = null;

                        while (true) {
                            // Get the next chunk posWeakChunk
                            final int X, Z;
                            synchronized (chunksIter) {
                                if (!chunksIter.hasNext()) break;
                                final BlockVector2 pos = chunksIter.next();
                                X = pos.getX();
                                Z = pos.getZ();
                            }
                            if (!newFilter.appliesChunk(X, Z)) {
                                continue;
                            }
                            IChunk chunk = queue.getCachedChunk(X, Z);
                            // Initialize
                            chunk.init(queue, X, Z);

                            IChunk newChunk = newFilter.applyChunk(chunk, region);
                            if (newChunk != null) {
                                chunk = newChunk;
                                if (block == null) block = queue.initFilterBlock();
                                chunk.filterBlocks(newFilter, block, region);
                            }
                            queue.submit(chunk);
                        }
                        queue.flush();
                    }
                }
            });
        }
        // Join filters
        for (int i = 0; i < tasks.length; i++) {
            final ForkJoinTask task = tasks[i];
            if (task != null) {
                task.quietlyJoin();
            }
        }
        filter.join();
        return filter;
    }

    public int getChanges() {
        return -1;
    }

    @Override
    public int countBlocks(final Region region, final Mask searchMask) {
        return
                // Apply a filter over a region
                apply(region, searchMask
                .toFilter(new CountFilter())) // Adapt the mask to a filter which counts
                .getParent() // Get the counter of this mask
                .getTotal(); // Get the total from the counter
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        apply(region, block);
        return getChanges();
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        apply(region, pattern);
        return getChanges();
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        if (vset instanceof Region) {
            setBlocks((Region) vset, pattern);
        }
        for (BlockVector3 blockVector3 : vset) {
            pattern.apply(this, blockVector3, blockVector3);
        }
        return getChanges();
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        apply(region, mask.toFilter(pattern));
        return getChanges();
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        return apply(region, new DistrFilter()).getDistribution();
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        return apply(region, new DistrFilter()).getTypeDistribution();
    }
}
