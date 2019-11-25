/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.object.HistoryExtent;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.LightingExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
public class AbstractDelegateExtent implements Extent, LightingExtent {

    public Extent extent;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public AbstractDelegateExtent(Extent extent) {
        checkNotNull(extent);
        this.extent = extent;
    }

    /**
     * Get the extent.
     *
     * @return the extent
     */
    public Extent getExtent() {
        return extent;
    }

    /*
    Bounds
     */

    @Override
    public BlockState getBlock(BlockVector3 position) {
       return extent.getBlock(position.getX(),position.getY(),position.getZ());
    }

    /*
        Queue based methods
        TODO NOT IMPLEMENTED: IQueueExtent and such need to implement these
         */
    @Override
    public boolean isQueueEnabled() {
        return getExtent().isQueueEnabled();
    }

    @Override
    public void disableQueue() {
        try {
            if (!(getExtent() instanceof ForgetfulExtentBuffer)) { // placeholder
                getExtent().disableQueue();
            }
        } catch (FaweException ignored) {
        }
        if (getExtent() instanceof AbstractDelegateExtent) {
            Extent next = ((AbstractDelegateExtent) getExtent()).getExtent();
            new ExtentTraverser(this).setNext(next);
        } else {
            getLogger(AbstractDelegateExtent.class).debug("Cannot disable queue");
        }
    }

    @Override
    public void enableQueue() {
        try {
            getExtent().enableQueue();
        } catch (FaweException enableQueue) {
            // TODO NOT IMPLEMENTED - THIS IS IMPORTANT (ForgetfulExtentBuffer is just a placeholder for now, it won't work)
            new ExtentTraverser<>(this).setNext(new ForgetfulExtentBuffer(getExtent()));
        }
    }

    /*
     History
     */
    public void setChangeSet(FaweChangeSet changeSet) {
        if (extent instanceof HistoryExtent) {
            HistoryExtent history = ((HistoryExtent) extent);
            if (changeSet == null) {
                new ExtentTraverser(this).setNext(history.getExtent());
            } else {
                history.setChangeSet(changeSet);
            }
        } else if (extent instanceof AbstractDelegateExtent) {
            ((AbstractDelegateExtent) extent).setChangeSet(changeSet);
        } else if (changeSet != null) {
            new ExtentTraverser<>(this).setNext(new HistoryExtent(extent, changeSet));
        }
    }

    @Override
    public int getMaxY() {
        return getExtent().getMaxY();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getExtent().getBlock(x, y, z);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return extent.createEntity(location, entity);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return extent.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return extent.getEntities(region);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return extent.getBiome(position);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getExtent().getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return getExtent().getBiomeType(x, z);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
        throws WorldEditException {
        return getExtent().setBlock(position.getX(), position.getY(), position.getZ(), block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
        throws WorldEditException {
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        setBlock(x, y, z, getBlock(x, y, z).toBaseBlock(tile));
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return getExtent().setBiome(position.getX(), 0, position.getZ(), biome);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        return extent.setBlock(position, block);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        if (getExtent() instanceof LightingExtent) {
            return ((LightingExtent) getExtent()).getSkyLight(x, y, z);
        }
        return 0;
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        if (getExtent() instanceof LightingExtent) {
            return ((LightingExtent) getExtent()).getBlockLight(x, y, z);
        }
        return getBrightness(x, y, z);
    }
    @Override
    public int getOpacity(int x, int y, int z) {
        if (getExtent() instanceof LightingExtent) {
            return ((LightingExtent) getExtent()).getOpacity(x, y, z);
        }
        return getBlock(x, y, z).getBlockType().getMaterial().getLightOpacity();
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (getExtent() instanceof LightingExtent) {
            return ((LightingExtent) getExtent()).getLight(x, y, z);
        }
        return 0;
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        if (getExtent() instanceof LightingExtent) {
            return ((LightingExtent) getExtent()).getBrightness(x, y, z);
        }
        return getBlock(x, y, z).getBlockType().getMaterial().getLightValue();
    }

    /*
    Generic
     */

    @Override
    public String toString() {
        return super.toString() + ":" + (extent == this ? "" : extent.toString());
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return getExtent().getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return getExtent().getMaximumPoint();
    }

    protected Operation commitBefore() {
        return null;
    }

    @Override
    public @Nullable Operation commit() {
        Operation ours = commitBefore();
        Operation other = null;
        if (getExtent() != this) {
            other = getExtent().commit();
        }
        if (ours != null && other != null) {
            return new OperationQueue(ours, other);
        } else if (ours != null) {
            return ours;
        } else if (other != null) {
            return other;
        } else {
            return null;
        }
    }

    @Override
    public Extent addProcessor(IBatchProcessor processor) {
        Extent result = this.extent.addProcessor(processor);
        if (result != this.extent) {
            new ExtentTraverser<Extent>(this).setNext(result);
        }
        return this;
    }

    @Override
    public Extent disableHistory() {
        Extent result = this.extent.disableHistory();
        if (result != this.extent) {
            new ExtentTraverser<Extent>(this).setNext(result);
        }
        return this;
    }
}
