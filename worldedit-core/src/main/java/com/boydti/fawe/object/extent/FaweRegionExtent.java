package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.Collection;
import javax.annotation.Nullable;

public abstract class FaweRegionExtent extends ResettableExtent {
    private final FaweLimit limit;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public FaweRegionExtent(Extent extent, FaweLimit limit) {
        super(extent);
        this.limit = limit;
    }

    public abstract boolean contains(int x, int y, int z);

    public abstract boolean contains(int x, int z);

    public abstract Collection<Region> getRegions();

    public boolean isGlobal() {
        return getRegions().stream().anyMatch(Region::isGlobal);
    }

    @Override
    public final boolean contains(BlockVector3 p) {
        return contains(p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    public final boolean contains(BlockVector2 p) {
        return contains(p.getBlockX(), p.getBlockZ());
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return false;
        }
        return super.setBlock(x, y, z, block);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return false;
        }
        return super.setBiome(x, y, z, biome);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return null;
        }
        return super.getBiome(position);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        if (!contains(x, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return null;
        }
        return super.getBiomeType(x, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        return super.getFullBlock(position);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return BlockTypes.AIR.getDefaultState();
        }
        return super.getBlock(position);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return 0;
        }
        return super.getBlockLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return 0;
        }
        return super.getBrightness(x, y, z);
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return 0;
        }
        return super.getLight(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return 0;
        }
        return super.getOpacity(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return 0;
        }
        return super.getSkyLight(x, y, z);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        if (!contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, FaweException.OUTSIDE_REGION);
            }
            return null;
        }
        return super.createEntity(location, entity);
    }
}
