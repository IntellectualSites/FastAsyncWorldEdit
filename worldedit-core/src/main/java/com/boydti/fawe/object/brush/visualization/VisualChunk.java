package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.blocks.BitSetBlocks;
import com.boydti.fawe.beta.implementation.holder.ChunkHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.concurrent.Future;

/**
 * FAWE visualizations display glass (20) as a placeholder
 * - Using a non transparent block can cause FPS lag
 */
public class VisualChunk extends ChunkHolder {
    public static BlockState VISUALIZE_BLOCK = BlockTypes.BLACK_STAINED_GLASS.getDefaultState();
    private final IChunk parent;
    private final VisualExtent extent;

    public VisualChunk(IChunk parent, VisualExtent extent) {
        this.parent = parent;
        this.extent = extent;
    }

    public IChunk getParent() {
        return parent;
    }

    @Override
    public Future call() {
        return extent.sendChunkUpdate(this);
    }

    @Override
    public IChunkGet get() {
        if (parent instanceof ChunkHolder) {
            return ((ChunkHolder) parent).get();
        }
        return parent;
    }

    @Override
    public IChunkSet set() {
        return new BitSetBlocks(VISUALIZE_BLOCK);
    }
}
