package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class ProcessedWEExtent extends AbstractDelegateExtent {

    private final FaweLimit limit;
    private final Extent extent;

    public ProcessedWEExtent(Extent parent, FaweLimit limit) {
        super(parent);
        this.limit = limit;
        this.extent = parent;
    }

    public void setLimit(FaweLimit other) {
        this.limit.set(other);
    }

    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        if (entity == null) {
            return null;
        }
        if (!limit.MAX_ENTITIES()) {
            WEManager.IMP.cancelEditSafe(this, FaweCache.MAX_ENTITIES);
            return null;
        }
        return super.createEntity(location, entity);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        if (!limit.MAX_CHECKS()) {
            WEManager.IMP.cancelEditSafe(this, FaweCache.MAX_CHECKS);
            return BlockTypes.AIR.getDefaultState();
        } else {
            return extent.getBlock(x, y, z);
        }
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 pos) {
        if (!limit.MAX_CHECKS()) {
            WEManager.IMP.cancelEditSafe(this, FaweCache.MAX_CHECKS);
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        } else {
            return extent.getFullBlock(pos);
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block)
        throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Override
    public BlockState getBlock(BlockVector3 location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block)
        throws WorldEditException {
        boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
        if (hasNbt) {
            if (!limit.MAX_BLOCKSTATES()) {
                WEManager.IMP.cancelEdit(this, FaweCache.MAX_TILES);
                return false;
            } else {
                if (!limit.MAX_CHANGES()) {
                    WEManager.IMP.cancelEdit(this, FaweCache.MAX_CHANGES);
                    return false;
                }
                return extent.setBlock(x, y, z, block);
            }
        }
        if (!limit.MAX_CHANGES()) {
            WEManager.IMP.cancelEdit(this, FaweCache.MAX_CHANGES);
            return false;
        } else {
            return extent.setBlock(x, y, z, block);
        }
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        if (!limit.MAX_CHANGES()) {
            WEManager.IMP.cancelEditSafe(this, FaweCache.MAX_CHANGES);
            return false;
        }
        return super.setBiome(position, biome);
    }
}
