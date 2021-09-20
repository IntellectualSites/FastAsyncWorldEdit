package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DisallowedBlocksExtent extends AbstractDelegateExtent implements IBatchProcessor {

    private Set<String> states = null;
    private Set<String> blocks = null;

    /**
     * Create a new instance.
     *
     * @param extent         the extent
     * @param checkingBlocks if the configured disallowed-blocks in config-legacy.yml should be removed
     * @param states         blockstates to disallow
     */
    public DisallowedBlocksExtent(Extent extent, boolean checkingBlocks, Set<String> states) {
        super(extent);
        if (checkingBlocks) {
            blocks = WorldEdit.getInstance().getConfiguration().disallowedBlocks.stream().map(String::toLowerCase).
                    collect(Collectors.toSet());
        }
        if (states != null && !states.isEmpty()) {
            this.states = new HashSet<>(states)
                    .stream()
                    .map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return super.setBlock(location, (block));
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return super.setBlock(x, y, z, stripBlockNBT(block));
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {

        return set;
    }

    @Override
    public Future<IChunkSet> postProcessSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return CompletableFuture.completedFuture(set);
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
        return ProcessorScope.CHANGING_BLOCKS;
    }

}
