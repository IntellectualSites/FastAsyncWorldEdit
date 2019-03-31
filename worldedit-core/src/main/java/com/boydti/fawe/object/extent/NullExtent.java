package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NullExtent extends FaweRegionExtent {

    private final BBC reason;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public NullExtent(Extent extent, BBC failReason) {
        super(extent, FaweLimit.MAX);
        this.reason = failReason;
    }

    public NullExtent() {
        super(new com.sk89q.worldedit.extent.NullExtent(), FaweLimit.MAX);
        this.reason = BBC.WORLDEDIT_CANCEL_REASON_MANUAL;
    }
    
    @Override
    public ResettableExtent setExtent(Extent extent) {
        return this;
    }

    @Override
    public BaseBiome getBiome(final BlockVector2 arg0) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return null;
        }
    }

    @Override
    public BlockState getBlock(final BlockVector3 arg0) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return null;
        }
    }

    @Override
    public BlockState getLazyBlock(final BlockVector3 arg0) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return null;
        }
    }

    @Override
    public boolean setBiome(final BlockVector2 arg0, final BaseBiome arg1) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return false;
        }
    }

    @Override
    public boolean setBlock(final BlockVector3 arg0, final BlockStateHolder arg1) throws WorldEditException {
        if (reason != null) {
            throw new FaweException(reason);
        } else {
            return false;
        }
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return null;
        }
    }

    @Override
    public Entity createEntity(final Location arg0, final BaseEntity arg1) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return null;
        }
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Entity> getEntities(final Region arg0) {
        return new ArrayList<>();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(0, 0, 0);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(0, 0, 0);
    }

    @Override
    public boolean contains(int x, int z) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return false;
        }
    }

    @Override
    public boolean contains(int x, int y, int z) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return false;
        }
    }

    @Override
    public Collection<Region> getRegions() {
        return Collections.emptyList();
    }

    @Override
    protected Operation commitBefore() {
        return null;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return -1;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return -1;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        if(reason != null) {
        	throw new FaweException(reason);
        }else {
        	return -1;
        }
    }

    @Override
    public Extent getExtent() {
        return this;
    }
}
