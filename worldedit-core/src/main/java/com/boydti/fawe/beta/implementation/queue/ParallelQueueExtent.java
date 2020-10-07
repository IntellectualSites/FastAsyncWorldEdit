package com.boydti.fawe.beta.implementation.queue;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.IQueueWrapper;
import com.boydti.fawe.beta.implementation.filter.CountFilter;
import com.boydti.fawe.beta.implementation.filter.DistrFilter;
import com.boydti.fawe.beta.implementation.filter.LinkedFilter;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.beta.implementation.processors.BatchProcessorHolder;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.WorldCopyClipboard;
import com.boydti.fawe.object.extent.NullExtent;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.PassthroughExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

public class ParallelQueueExtent extends PassthroughExtent implements IQueueWrapper {

    private final World world;
    private final QueueHandler handler;
    private final BatchProcessorHolder processor;
    private final BatchProcessorHolder postProcessor;
    private int changes;
    private final boolean fastmode;

    public ParallelQueueExtent(QueueHandler handler, World world, boolean fastmode) {
        super(handler.getQueue(world, new BatchProcessorHolder(), new BatchProcessorHolder()));
        this.world = world;
        this.handler = handler;
        this.processor = (BatchProcessorHolder) getExtent().getProcessor();
        this.postProcessor = (BatchProcessorHolder) getExtent().getPostProcessor();
        this.fastmode = fastmode;
    }

    @Override
    public IQueueExtent<IQueueChunk> getExtent() {
        return (IQueueExtent<IQueueChunk>) super.getExtent();
    }

    @Override
    public boolean cancel() {
        if (super.cancel()) {
            processor.setProcessor(new NullExtent(this, FaweCache.MANUAL));
            postProcessor.setPostProcessor(new NullExtent(this, FaweCache.MANUAL));
            return true;
        }
        return false;
    }

    private IQueueExtent<IQueueChunk> getNewQueue() {
        return wrapQueue(handler.getQueue(this.world, this.processor, this.postProcessor));
    }

    @Override
    public IQueueExtent<IQueueChunk> wrapQueue(IQueueExtent<IQueueChunk> queue) {
        // TODO wrap
        queue.setProcessor(this.processor);
        queue.setPostProcessor(this.postProcessor);
        return queue;
    }

    @Override
    public <T extends Filter> T apply(Region region, T filter, boolean full) {
        // The chunks positions to iterate over
        final Set<BlockVector2> chunks = region.getChunks();
        final Iterator<BlockVector2> chunksIter = chunks.iterator();

        // Get a pool, to operate on the chunks in parallel
        final int size = Math.min(chunks.size(), Settings.IMP.QUEUE.PARALLEL_THREADS);
        if (size <= 1 && chunksIter.hasNext()) {
            BlockVector2 pos = chunksIter.next();
            getExtent().apply(null, filter, region, pos.getX(), pos.getZ(), full);
        } else {
            final ForkJoinTask[] tasks = IntStream.range(0, size).mapToObj(i -> handler.submit(() -> {
                try {
                    final Filter newFilter = filter.fork();
                    // Create a chunk that we will reuse/reset for each operation
                    final IQueueExtent<IQueueChunk> queue = getNewQueue();
                    queue.setFastMode(fastmode);
                    synchronized (queue) {
                        ChunkFilterBlock block = null;

                        while (true) {
                            // Get the next chunk posWeakChunk
                            final int chunkX;
                            final int chunkZ;
                            synchronized (chunksIter) {
                                if (!chunksIter.hasNext()) {
                                    break;
                                }
                                final BlockVector2 pos = chunksIter.next();
                                chunkX = pos.getX();
                                chunkZ = pos.getZ();
                            }
                            block = queue.apply(block, newFilter, region, chunkX, chunkZ, full);
                        }
                        queue.flush();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            })).toArray(ForkJoinTask[]::new);
            // Join filters
            for (ForkJoinTask task : tasks) {
                if (task != null) {
                    task.quietlyJoin();
                }
            }
            filter.join();
        }
        return filter;
    }

    @Override
    public int countBlocks(Region region, Mask searchMask) {
        return
            // Apply a filter over a region
            apply(region, searchMask
                .toFilter(new CountFilter()), false) // Adapt the mask to a filter which counts
                .getParent() // Get the counter of this mask
                .getTotal(); // Get the total from the counter
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        return this.changes = apply(region, new BlockMaskBuilder().add(block).build(this).toFilter(new CountFilter())).getParent().getTotal();
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return this.changes = apply(region, new LinkedFilter<>(pattern, new CountFilter()), true).getChild().getTotal();
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        if (vset instanceof Region) {
            this.changes = setBlocks((Region) vset, pattern);
        }
        // TODO optimize parallel
        for (BlockVector3 blockVector3 : vset) {
            if (pattern.apply(this, blockVector3, blockVector3)) {
                this.changes++;
            }
        }
        return this.changes;
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern)
        throws MaxChangedBlocksException {
        boolean full = mask.replacesAir();
        return this.changes = apply(region, mask.toFilter(pattern), full).getBlocksApplied();
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        return apply(region, new DistrFilter(), false).getDistribution();
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        return apply(region, new DistrFilter(), false).getTypeDistribution();
    }

    /**
     * Lazily copy a region
     *
     * @param region
     * @return
     */
    @Override
    public Clipboard lazyCopy(Region region) {
        Clipboard clipboard = new WorldCopyClipboard(() -> this, region);
        clipboard.setOrigin(region.getMinimumPoint());
        return clipboard;
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region the region
     * @param searchBlocks the list of blocks to search
     * @return the number of blocks that matched the block
     */
    @Override
    public int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        Mask mask = new BlockMask(this, searchBlocks);
        return countBlocks(region, mask);
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        return replaceBlocks(region, filter, new BlockPattern(replacement));
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @Override
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        Mask mask = filter == null ? new ExistingBlockMask(this) : new BlockMask(this, filter);
        return replaceBlocks(region, mask, pattern);
    }
}
