package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.util.WEManager;
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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public abstract class FaweRegionExtent extends ResettableExtent implements IBatchProcessor {

    @Nullable
    private final FaweLimit limit;
    private final boolean hasLimit;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public FaweRegionExtent(Extent extent, @Nullable FaweLimit limit) {
        super(extent);
        this.limit = limit;
        this.hasLimit = limit != null;
    }

    @Override
    public abstract boolean contains(int x, int y, int z);

    public abstract boolean contains(int x, int z);

    public abstract Collection<Region> getRegions();

    public boolean isGlobal() {
        for (Region r : getRegions()) {
            if (r.isGlobal()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Extent construct(Extent child) {
        if (getExtent() != child) {
            new ExtentTraverser<Extent>(this).setNext(child);
        }
        return this;
    }

    @Override
    public final boolean contains(BlockVector3 p) {
        return contains(p.x(), p.y(), p.z());
    }

    public final boolean contains(BlockVector2 p) {
        return contains(p.x(), p.z());
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block)
            throws WorldEditException {
        if (!contains(x, y, z)) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return false;
        }
        return super.setBlock(x, y, z, block);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (!contains(x, y, z)) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return false;
        }
        return super.setBiome(x, y, z, biome);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return getBiomeType(position.x(), position.y(), position.z());
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return null;
        }
        return super.getBiomeType(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getFullBlock(position.x(), position.y(), position.z());
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        return super.getFullBlock(x, y, z);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return getBlock(position.x(), position.y(), position.z());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return BlockTypes.AIR.getDefaultState();
        }
        return super.getBlock(x, y, z);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        if (!contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return null;
        }
        return super.createEntity(location, entity);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        if (!contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            if (hasLimit && !limit.MAX_FAILS()) {
                WEManager.weManager().cancelEditSafe(this, FaweCache.OUTSIDE_REGION);
            }
            return null;
        }
        return super.createEntity(location, entity, uuid);
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.REMOVING_BLOCKS;
    }

}
