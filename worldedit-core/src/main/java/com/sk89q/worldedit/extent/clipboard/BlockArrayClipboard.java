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

package com.sk89q.worldedit.extent.clipboard;

<<<<<<< HEAD
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.boydti.fawe.object.extent.LightingExtent;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
=======
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.WorldEditException;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
<<<<<<< HEAD
=======
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockState;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores block data as a multi-dimensional array of {@link BlockState}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard implements Clipboard, LightingExtent, Closeable {

<<<<<<< HEAD
    private Region region;
    public FaweClipboard IMP;
    private Vector size;
    private int mx;
    private int my;
    private int mz;
    private Vector origin;
    private MutableBlockVector mutable = new MutableBlockVector();

    public BlockArrayClipboard(Region region) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.IMP = Settings.IMP.CLIPBOARD.USE_DISK ? new DiskOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ()) : new MemoryOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ());
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }
=======
    private final Region region;
    private BlockVector3 origin;
    private final BlockStateHolder[][][] blocks;
    private final List<ClipboardEntity> entities = new ArrayList<>();
>>>>>>> 399e0ad5... Refactor vector system to be cleaner

    /**
     * Create a new instance.
     * <p>
     * <p>The origin will be placed at the region's lowest minimum point.</p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region, UUID clipboardId) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.IMP = Settings.IMP.CLIPBOARD.USE_DISK ? new DiskOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ(), clipboardId) : new MemoryOptimizedClipboard(size.getBlockX(), size.getBlockY(), size.getBlockZ());
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }

<<<<<<< HEAD
    public BlockArrayClipboard(Region region, FaweClipboard clipboard) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
        this.IMP = clipboard;
    }

    public void init(Region region, FaweClipboard fc) {
        checkNotNull(region);
        checkNotNull(fc);
        this.region = region.clone();
        this.size = getDimensions();
        this.IMP = fc;
        this.origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    public void close() {
        IMP.close();
=======
        BlockVector3 dimensions = getDimensions();
        blocks = new BlockStateHolder[dimensions.getBlockX()][dimensions.getBlockY()][dimensions.getBlockZ()];
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        this.origin = origin;
        IMP.setOrigin(origin.subtract(region.getMinimumPoint()));
    }

    @Override
    public BlockVector3 getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
<<<<<<< HEAD
        List<Entity> filtered = new ArrayList<Entity>();
        for (Entity entity : getEntities()) {
            if (region.contains(entity.getLocation().toVector())) {
=======
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : entities) {
            if (region.contains(entity.getLocation().toVector().toBlockPoint())) {
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
                filtered.add(entity);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return IMP.getEntities();
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return IMP.createEntity(location.getExtent(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), entity);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (region.contains(position)) {
<<<<<<< HEAD
            int x = position.getBlockX() - mx;
            int y = position.getBlockY() - my;
            int z = position.getBlockZ() - mz;
            return IMP.getBlock(x, y, z);
=======
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            BlockStateHolder block = blocks[v.getBlockX()][v.getBlockY()][v.getBlockZ()];
            if (block != null) {
                return block.toImmutableState();
            }
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
        }
        return EditSession.nullBlock;
    }

    public BlockState getBlockAbs(int x, int y, int z) {
        return IMP.getBlock(x, y, z);
    }

    @Override
<<<<<<< HEAD
    public BlockState getLazyBlock(Vector position) {
        return getBlock(position);
    }
=======
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (region.contains(position)) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            BlockStateHolder block = blocks[v.getBlockX()][v.getBlockY()][v.getBlockZ()];
            if (block != null) {
                return block.toBaseBlock();
            }
        }
>>>>>>> 399e0ad5... Refactor vector system to be cleaner

    @Override
    public BlockState getFullBlock(Vector position) {
        return getLazyBlock(position);
    }

    @Override
<<<<<<< HEAD
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        if (region.contains(location)) {
            final int x = location.getBlockX();
            final int y = location.getBlockY();
            final int z = location.getBlockZ();
            return setBlock(x, y, z, block);
=======
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        if (region.contains(position)) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            blocks[v.getBlockX()][v.getBlockY()][v.getBlockZ()] = block;
            return true;
        } else {
            return false;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
        }
        return false;
    }

    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        x -= mx;
        y -= my;
        z -= mz;
        return IMP.setTile(x, y, z, tag);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        x -= mx;
        y -= my;
        z -= mz;
        return IMP.setBlock(x, y, z, block);
    }

    @Override
<<<<<<< HEAD
    public BaseBiome getBiome(Vector2D position) {
        int x = position.getBlockX() - mx;
        int z = position.getBlockZ() - mz;
        return IMP.getBiome(x, z);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        int x = position.getBlockX() - mx;
        int z = position.getBlockZ() - mz;
        IMP.setBiome(x, z, biome.getId());
        return true;
=======
    public BaseBiome getBiome(BlockVector2 position) {
        return new BaseBiome(0);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        return false;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }



    @Override
    public int getLight(int x, int y, int z) {
        return getBlockLight(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return getBlock(mutable).getBlockType().getMaterial().getLightOpacity();
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return getBlock(mutable).getBlockType().getMaterial().getLightValue();
    }
}