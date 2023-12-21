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

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.HistoryExtent;
import com.fastasyncworldedit.core.extent.NullExtent;
import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
//FAWE start - made none abstract
public class AbstractDelegateExtent implements Extent {
//FAWE end

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    //FAWE start - made public: Not safe for public usage
    public Extent extent;
    //FAWE end

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
        //FAWE start - return coordinates
        return extent.getBlock(position.getX(), position.getY(), position.getZ());
        //FAWE end
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return extent.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        //FAWE start - return coordinates
        return extent.getFullBlock(position.getX(), position.getY(), position.getZ());
        //FAWE end
    }

    //FAWE start
    /*
        Queue based methods
        TODO NOT IMPLEMENTED: IQueueExtent and such need to implement these
     */
    //FAWE end

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        //FAWE start - return coordinates
        return extent.getFullBlock(x, y, z);
        //FAWE end
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

    //FAWE start
    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        return extent.createEntity(location, entity, uuid);
    }
    //FAWE end

    @Override
    @Nullable
    public Operation commit() {
        Operation ours = commitBefore();
        Operation other = null;
        //FAWE start - implement extent
        if (extent != this) {
            other = extent.commit();
        }
        //FAWE end
        if (ours != null && other != null) {
            return new OperationQueue(ours, other);
        } else if (ours != null) {
            return ours;
        } else {
            return other;
        }
    }

    //FAWE start
    @Override
    public boolean cancel() {
        ExtentTraverser<Extent> traverser = new ExtentTraverser<>(this);

        NullExtent nullExtent = new NullExtent(getExtent(), FaweCache.MANUAL);

        ExtentTraverser<Extent> next = traverser.next();
        if (next != null) {
            Extent child = next.get();
            if (child instanceof NullExtent) {
                return true;
            }
            traverser.setNext(nullExtent);
            child.cancel();
        }
        addProcessor(nullExtent);
        addPostProcessor(nullExtent);
        return true;
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
            LOGGER.error("Cannot disable queue");
        }
    }

    @Override
    public boolean isWorld() {
        return extent.isWorld();
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(final Region region) {
        return extent.getBlockDistribution(region);
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(final Region region) {
        return extent.getBlockDistributionWithData(region);
    }

    @Override
    public int getMaxY() {
        return extent.getMaxY();
    }

    @Override
    public int countBlocks(final Region region, final Set<BaseBlock> searchBlocks) {
        return extent.countBlocks(region, searchBlocks);
    }

    @Override
    public int countBlocks(final Region region, final Mask searchMask) {
        return extent.countBlocks(region, searchMask);
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(final Region region, final B block) throws MaxChangedBlocksException {
        return extent.setBlocks(region, block);
    }

    @Override
    public int setBlocks(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        return extent.setBlocks(region, pattern);
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(
            final Region region,
            final Set<BaseBlock> filter,
            final B replacement
    )
            throws MaxChangedBlocksException {
        return extent.replaceBlocks(region, filter, replacement);
    }

    @Override
    public int replaceBlocks(final Region region, final Set<BaseBlock> filter, final Pattern pattern) throws
            MaxChangedBlocksException {
        return extent.replaceBlocks(region, filter, pattern);
    }

    @Override
    public int replaceBlocks(final Region region, final Mask mask, final Pattern pattern) throws MaxChangedBlocksException {
        return extent.replaceBlocks(region, mask, pattern);
    }

    @Override
    public int setBlocks(final Set<BlockVector3> vset, final Pattern pattern) {
        return extent.setBlocks(vset, pattern);
    }

    @Override
    public int getMinY() {
        return extent.getMinY();
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
        if (Settings.settings().EXPERIMENTAL.OTHER) {
            LOGGER.info("addProcessor Info: \t " + processor.getClass().getName());
            LOGGER.info("The following is not an error or a crash:");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                LOGGER.info(stackTraceElement.toString());
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
        if (Settings.settings().EXPERIMENTAL.OTHER) {
            LOGGER.info("addPostProcessor Info: \t " + processor.getClass().getName());
            LOGGER.info("The following is not an error or a crash:");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                LOGGER.info(stackTraceElement.toString());
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

    @Override
    public <T extends Filter> T apply(final Region region, final T filter, final boolean full) {
        return extent.apply(region, filter, full);
    }
    //FAWE end

    protected Operation commitBefore() {
        return null;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        //FAWE start - switch top x,y,z
        return extent.getBiomeType(position.getX(), position.getY(), position.getZ());
        //FAWE end
    }

    //FAWE start
    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return extent.getBiomeType(x, y, z);
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        return extent.getEmittedLight(x, y, z);
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
    //FAWE end

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
            throws WorldEditException {
        //FAWE start - switch to x,y,z
        return extent.setBlock(position.getX(), position.getY(), position.getZ(), block);
        //FAWE end
    }

    //FAWE start
    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(
            int x, int y,
            int z, T block
    ) throws WorldEditException {
        return extent.setBlock(x, y, z, block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return setBlock(x, y, z, getBlock(x, y, z).toBaseBlock(tile));
    }
    //FAWE end

    @Override
    public boolean fullySupports3DBiomes() {
        return extent.fullySupports3DBiomes();
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        //FAWE start - switch to x,y,z
        return extent.setBiome(position.getX(), position.getY(), position.getZ(), biome);
        //FAWE end
    }

    //FAWE start
    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return extent.setBiome(x, y, z, biome);
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
    //FAWE end
}
