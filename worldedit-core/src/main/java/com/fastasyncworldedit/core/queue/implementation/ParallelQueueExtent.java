package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.NullExtent;
import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.fastasyncworldedit.core.extent.clipboard.WorldCopyClipboard;
import com.fastasyncworldedit.core.extent.filter.CountFilter;
import com.fastasyncworldedit.core.extent.filter.DistrFilter;
import com.fastasyncworldedit.core.extent.filter.LinkedFilter;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.BatchProcessorHolder;
import com.fastasyncworldedit.core.extent.processor.MultiBatchProcessor;
import com.fastasyncworldedit.core.function.mask.BlockMaskBuilder;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

public class ParallelQueueExtent extends PassthroughExtent {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final ThreadLocal<Extent> extents = new ThreadLocal<>();

    private final World world;
    private final QueueHandler handler;
    private final BatchProcessorHolder processor;
    private final BatchProcessorHolder postProcessor;
    // Array for lazy avoidance of concurrent modification exceptions and needless overcomplication of code (synchronisation is
    // not very important)
    private final boolean[] faweExceptionReasonsUsed = new boolean[FaweException.Type.values().length];
    private final boolean fastmode;
    private int changes;
    private int lastException = Integer.MIN_VALUE;
    private int exceptionCount = 0;

    public ParallelQueueExtent(QueueHandler handler, World world, boolean fastmode) {
        super(handler.getQueue(world, new BatchProcessorHolder(), new BatchProcessorHolder()));
        this.world = world;
        this.handler = handler;
        this.processor = (BatchProcessorHolder) getExtent().getProcessor();
        if (this.processor.getProcessor() instanceof MultiBatchProcessor) {
            ((MultiBatchProcessor) this.processor.getProcessor()).setFaweExceptionArray(faweExceptionReasonsUsed);
        }
        this.postProcessor = (BatchProcessorHolder) getExtent().getPostProcessor();
        if (this.postProcessor.getProcessor() instanceof MultiBatchProcessor) {
            ((MultiBatchProcessor) this.postProcessor.getProcessor()).setFaweExceptionArray(faweExceptionReasonsUsed);
        }
        this.fastmode = fastmode;
    }

    /**
     * Removes the extent currently associated with the calling thread.
     */
    public static void clearCurrentExtent() {
        extents.remove();
    }

    /**
     * Sets the extent associated with the calling thread.
     */
    public static void setCurrentExtent(Extent extent) {
        extents.set(extent);
    }

    private void enter(Extent extent) {
        setCurrentExtent(extent);
    }

    private void exit() {
        clearCurrentExtent();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IQueueExtent<IQueueChunk> getExtent() {
        Extent extent = extents.get();
        if (extent == null) {
            extent = super.getExtent();
        }
        return (IQueueExtent<IQueueChunk>) extent;
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

    @SuppressWarnings("rawtypes")
    private IQueueExtent<IQueueChunk> getNewQueue() {
        return handler.getQueue(world, this.processor, this.postProcessor);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends Filter> T apply(Region region, T filter, boolean full) {
        // The chunks positions to iterate over
        final Set<BlockVector2> chunks = region.getChunks();
        final Iterator<BlockVector2> chunksIter = chunks.iterator();

        // Get a pool, to operate on the chunks in parallel
        final int size = Math.min(chunks.size(), Settings.settings().QUEUE.PARALLEL_THREADS);
        if (size <= 1) {
            // if PQE is ever used with PARALLEL_THREADS = 1, or only one chunk is edited, just run sequentially
            while (chunksIter.hasNext()) {
                BlockVector2 pos = chunksIter.next();
                getExtent().apply(null, filter, region, pos.getX(), pos.getZ(), full);
            }
        } else {
            final ForkJoinTask[] tasks = IntStream.range(0, size).mapToObj(i -> handler.submit(() -> {
                try {
                    final Filter newFilter = filter.fork();
                    // Create a chunk that we will reuse/reset for each operation
                    final SingleThreadQueueExtent queue = (SingleThreadQueueExtent) getNewQueue();
                    queue.setFastMode(fastmode);
                    queue.setFaweExceptionArray(faweExceptionReasonsUsed);
                    enter(queue);
                    synchronized (queue) {
                        try {
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
                        } catch (Throwable t) {
                            if (t instanceof FaweException) {
                                Fawe.handleFaweException(faweExceptionReasonsUsed, (FaweException) t, LOGGER);
                            } else if (t.getCause() instanceof FaweException) {
                                Fawe.handleFaweException(faweExceptionReasonsUsed, (FaweException) t.getCause(), LOGGER);
                            } else {
                                throw t;
                            }
                        }
                    }
                } catch (Throwable e) {
                    String message = e.getMessage();
                    int hash = message != null ? message.hashCode() : 0;
                    if (lastException != hash) {
                        lastException = hash;
                        exceptionCount = 0;
                        LOGGER.catching(e);
                    } else if (exceptionCount < Settings.settings().QUEUE.PARALLEL_THREADS) {
                        exceptionCount++;
                        LOGGER.warn(message);
                    }
                } finally {
                    exit();
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
                        .toFilter(new CountFilter()), searchMask.replacesAir()) // Adapt the mask to a filter which counts
                        .getParent() // Get the counter of this mask
                        .getTotal(); // Get the total from the counter
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        Mask mask = new BlockMaskBuilder().add(block).build(this).inverse();
        return this.changes = apply(region, mask.toFilter(block), mask.replacesAir())
                .getBlocksApplied();
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return this.changes = apply(region, new LinkedFilter<>(pattern, new CountFilter()), true).getChild().getTotal();
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        if (vset instanceof Region) {
            this.changes = setBlocks((Region) vset, pattern);
            return this.changes;
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
        return apply(region, new DistrFilter(), true).getDistribution();
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        return apply(region, new DistrFilter(), true).getTypeDistribution();
    }

    /**
     * Lazily copy a region
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
     * @param region       the region
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
     * @param region      the region to replace the blocks within
     * @param filter      a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws
            MaxChangedBlocksException {
        return replaceBlocks(region, filter, new BlockPattern(replacement));
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region  the region to replace the blocks within
     * @param filter  a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
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
