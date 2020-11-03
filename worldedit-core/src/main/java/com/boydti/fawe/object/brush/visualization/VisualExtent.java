package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.beta.IQueueExtent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;

public class VisualExtent extends AbstractDelegateExtent {
    public static final BlockType VISUALIZE_BLOCK_DEFAULT = BlockTypes.BLACK_STAINED_GLASS;
    private final BlockType visualizeBlock;
    private final Player player;

    public VisualExtent(Extent parent, Player player) {
        this(parent, player, VISUALIZE_BLOCK_DEFAULT);
    }

    public VisualExtent(Extent parent, Player player, BlockType visualizeBlock) {
        super(parent);
        this.visualizeBlock = visualizeBlock;
        this.player = player;
    }

    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        if (block.getMaterial().isAir()) {
            return super.setBlock(x, y, z, block);
        } else {
            return super.setBlock(x, y, z, visualizeBlock.getDefaultState());
        }
    }

    @Nullable
    @Override
    public Operation commit() {
        IQueueExtent queue = (IQueueExtent) getExtent();
        return null;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        // Do nothing
        return false;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        // Do nothing
        return false;
    }

    public void clear() {
        IQueueExtent queue = (IQueueExtent) getExtent();
        queue.cancel();
    }
}
