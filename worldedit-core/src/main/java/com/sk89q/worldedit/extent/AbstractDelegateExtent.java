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
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.function.generator.GenBase;
import com.fastasyncworldedit.core.function.generator.Resource;
import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
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
public abstract class AbstractDelegateExtent implements Extent {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    //FAWE start - made public: Not safe for public usage
    public Extent extent;
    //FAWE end

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    protected AbstractDelegateExtent(Extent extent) {
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

    @SuppressWarnings("deprecation")
    @Override
    public BiomeType getBiome(final BlockVector2 position) {
        return extent.getBiome(position);
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

    //FAWE start
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
    public boolean regenerateChunk(
            final int x,
            final int z,
            @Nullable final BiomeType type,
            @Nullable final Long seed
    ) {
        return extent.regenerateChunk(x, z, type, seed);
    }

    @Override
    public int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY) {
        return extent.getHighestTerrainBlock(x, z, minY, maxY);
    }

    @Override
    public int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY, final Mask filter) {
        return extent.getHighestTerrainBlock(x, z, minY, maxY, filter);
    }

    @Override
    public int getNearestSurfaceLayer(final int x, final int z, final int y, final int minY, final int maxY) {
        return extent.getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(
            final int x,
            final int z,
            final int y,
            final int minY,
            final int maxY,
            final int failedMin,
            final int failedMax,
            final Mask mask
    ) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, mask);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(
            final int x,
            final int z,
            final int y,
            final int minY,
            final int maxY,
            final boolean ignoreAir
    ) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, ignoreAir);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(final int x, final int z, final int y, final int minY, final int maxY) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(
            final int x,
            final int z,
            final int y,
            final int minY,
            final int maxY,
            final int failedMin,
            final int failedMax
    ) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(
            final int x,
            final int z,
            final int y,
            final int minY,
            final int maxY,
            final int failedMin,
            final int failedMax,
            final boolean ignoreAir
    ) {
        return extent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, ignoreAir);
    }

    @Override
    public void addCaves(final Region region) throws WorldEditException {
        extent.addCaves(region);
    }

    @Override
    public void generate(final Region region, final GenBase gen) throws WorldEditException {
        extent.generate(region, gen);
    }

    @Override
    public void addSchems(
            final Region region,
            final Mask mask,
            final List<ClipboardHolder> clipboards,
            final int rarity,
            final boolean rotate
    ) throws WorldEditException {
        extent.addSchems(region, mask, clipboards, rarity, rotate);
    }

    @Override
    public void spawnResource(final Region region, final Resource gen, final int rarity, final int frequency) throws
            WorldEditException {
        extent.spawnResource(region, gen, rarity, frequency);
    }

    @Override
    public boolean contains(final BlockVector3 pt) {
        return extent.contains(pt);
    }

    @Override
    public boolean contains(final int x, final int y, final int z) {
        return extent.contains(x, y, z);
    }

    @Override
    public void addOre(
            final Region region,
            final Mask mask,
            final Pattern material,
            final int size,
            final int frequency,
            final int rarity,
            final int minY,
            final int maxY
    ) throws WorldEditException {
        extent.addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    @Override
    public void addOres(final Region region, final Mask mask) throws WorldEditException {
        extent.addOres(region, mask);
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
    public Clipboard lazyCopy(final Region region) {
        return extent.lazyCopy(region);
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
    ) throws MaxChangedBlocksException {
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
    public int center(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        return extent.center(region, pattern);
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
    public Extent enableHistory(final AbstractChangeSet changeSet) {
        return extent.enableHistory(changeSet);
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

    @Override
    public <T extends Filter> T apply(final Iterable<BlockVector3> positions, final T filter) {
        return extent.apply(positions, filter);
    }

    protected Operation commitBefore() {
        return null;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return extent.getBiomeType(x, y, z);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return extent.getBiome(position);
    }

    @Override
    public int getEmittedLight(final BlockVector3 position) {
        return extent.getEmittedLight(position);
    }
    /*
     History
     */

    @Override
    public int getEmittedLight(int x, int y, int z) {
        return extent.getEmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(final MutableBlockVector3 position) {
        return extent.getSkyLight(position);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return extent.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(final MutableBlockVector3 position) {
        return extent.getBrightness(position);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return extent.getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(final MutableBlockVector3 position) {
        return extent.getOpacity(position);
    }

    @Override
    public int getOpacity(final int x, final int y, final int z) {
        return extent.getOpacity(x, y, z);
    }

    @Override
    public int[] getHeightMap(final HeightMapType type) {
        return extent.getHeightMap(type);
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

    @Override
    public boolean fullySupports3DBiomes() {
        return extent.fullySupports3DBiomes();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean setBiome(final BlockVector2 position, final BiomeType biome) {
        return extent.setBiome(position, biome);
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
    public void setBlockLight(final BlockVector3 position, final int value) {
        extent.setBlockLight(position, value);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {
        extent.setSkyLight(x, y, z, value);
    }

    @Override
    public void setSkyLight(final BlockVector3 position, final int value) {
        extent.setSkyLight(position, value);
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {
        extent.setSkyLight(x, y, z, value);
    }

    @Override
    public void setHeightMap(final HeightMapType type, final int[] heightMap) {
        extent.setHeightMap(type, heightMap);
    }

    @Override
    public String toString() {
        return super.toString() + ":" + (extent == this ? "" : extent.toString());
    }
    //FAWE end
}
