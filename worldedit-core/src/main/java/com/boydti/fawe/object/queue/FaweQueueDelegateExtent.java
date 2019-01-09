package com.boydti.fawe.object.queue;

import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2D;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import static com.google.common.base.Preconditions.checkNotNull;

public class FaweQueueDelegateExtent extends DelegateFaweQueue {
    private final FaweQueue parentQueue;
    private final Extent parentExtent;
//    private final MutableBlockVector2D mutable2d = new MutableBlockVector2D();

    public FaweQueueDelegateExtent(FaweQueue parentQueue, Extent parentExtent) {
        super(parentQueue);
        checkNotNull(parentExtent);
        checkNotNull(parentQueue);
        this.parentQueue = parentQueue;
        this.parentExtent = parentExtent;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId) {
        return setBlock(x, y, z, BlockState.getFromInternalId(combinedId));
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId, CompoundTag nbt) {
        if (nbt != null) {
            return setBlock(x, y, z, BaseBlock.getFromInternalId(combinedId, nbt));
        }
        return setBlock(x, y, z, BlockState.getFromInternalId(combinedId));
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z);
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getLazyBlock(x, y, z).getInternalId();
    }

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getLazyBlock(x, y, z).getNbtData();
    }

    @Override
    public int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException {
        return parentExtent.getBiome(new BlockVector2(x, z)).getId();
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        return parentExtent.setBiome(position, biome);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return parentExtent.getBlock(position);
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        return parentExtent.getBiome(position);
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return parentExtent.setBlock(position, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return parentExtent.setBlock(x, y, z, block);
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return parentExtent.getLazyBlock(position);
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return parentExtent.getLazyBlock(x, y, z);
    }


}
