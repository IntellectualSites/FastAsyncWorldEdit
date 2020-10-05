/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.HistoryExtent;
import com.boydti.fawe.object.changeset.AbstractChangeSet;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
public class AbstractDelegateExtent implements Extent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDelegateExtent.class);

    //Not safe for public usage
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

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return extent.getBlock(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return extent.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return extent.getFullBlock(position.getX(), position.getY(), position.getZ());
    }

    /*
        Queue based methods
        TODO NOT IMPLEMENTED: IQueueExtent and such need to implement these
         */

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return extent.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return extent.getBiomeType(x, y, z);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return extent.getBiome(position);
    }
    /*
     History
     */

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        return extent.getEmmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return extent.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return extent.getBrightness(x, y, z);
    }

    public void setChangeSet(AbstractChangeSet changeSet) {
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
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
        throws WorldEditException {
        return extent.setBlock(position.getX(), position.getY(), position.getZ(), block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, @Range(from = 0, to = 255) int y,
        int z, T block) throws WorldEditException {
        return extent.setBlock(x, y, z, block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return setBlock(x, y, z, getBlock(x, y, z).toBaseBlock(tile));
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return extent.fullySupports3DBiomes();
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return extent.setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return extent.setBiome(position.getX(), position.getY(), position.getZ(), biome);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {
        extent.setSkyLight(x, y, z, value);
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {
        extent.setSkyLight(x, y, z, value);
    }

    @Override
    public String toString() {
        return super.toString() + ":" + (extent == this ? "" : extent.toString());
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return extent.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return extent.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return extent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return extent.getEntities();
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return extent.createEntity(location, entity);
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        extent.removeEntity(x, y, z, uuid);
    }

    @Override
    public boolean isQueueEnabled() {
        return extent.isQueueEnabled();
    }

    @Override
    public void enableQueue() {
        try {
            extent.enableQueue();
        } catch (FaweException enableQueue) {
            // TODO NOT IMPLEMENTED - THIS IS IMPORTANT (ForgetfulExtentBuffer is just a placeholder for now, it won't work)
            new ExtentTraverser<>(this).setNext(new ForgetfulExtentBuffer(extent));
        }
    }

    @Override
    public void disableQueue() {
        try {
            if (!(extent instanceof ForgetfulExtentBuffer)) { // placeholder
                extent.disableQueue();
            }
        } catch (FaweException ignored) {
        }
        if (extent instanceof AbstractDelegateExtent) {
            Extent next = ((AbstractDelegateExtent) extent).getExtent();
            new ExtentTraverser(this).setNext(next);
        } else {
            getLogger(AbstractDelegateExtent.class).debug("Cannot disable queue");
        }
    }

    @Override
    @Nullable
    public Operation commit() {
        Operation ours = commitBefore();
        Operation other = null;
        if (extent != this) {
            other = extent.commit();
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
    public int getMaxY() {
        return extent.getMaxY();
    }

    @Override
    public boolean relight(int x, int y, int z) {
        return extent.relight(x, y, z);
    }

    @Override
    public boolean relightBlock(int x, int y, int z) {
        return extent.relightBlock(x, y, z);
    }

    @Override
    public boolean relightSky(int x, int y, int z) {
        return extent.relightSky(x, y, z);
    }

    @Override
    public Extent addProcessor(IBatchProcessor processor) {
        if (Settings.IMP.EXPERIMENTAL.OTHER) {
            logger.info("addProcessor Info: \t " + processor.getClass().getName());
            logger.info("The following is not an error or a crash:");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                logger.info(stackTraceElement.toString());
            }

        }
        Extent result = this.extent.addProcessor(processor);
        if (result != this.extent) {
            new ExtentTraverser<Extent>(this).setNext(result);
        }
        return this;
    }

    @Override
    public Extent addPostProcessor(IBatchProcessor processor) {
        if (Settings.IMP.EXPERIMENTAL.OTHER) {
            logger.info("addPostProcessor Info: \t " + processor.getClass().getName());
            logger.info("The following is not an error or a crash:");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                logger.info(stackTraceElement.toString());
            }

        }
        Extent result = this.extent.addPostProcessor(processor);
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

    protected Operation commitBefore() {
        return null;
    }
}
