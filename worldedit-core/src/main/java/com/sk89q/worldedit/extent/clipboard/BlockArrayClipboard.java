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

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard.ClipboardEntity;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Stores block data as a multi-dimensional array of {@link BlockState}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard implements Clipboard, Closeable {

    private Region region;
    private BlockVector3 origin;
    public FaweClipboard IMP;
    private BlockVector3 size;
    private int mx;
    private int my;
    private int mz;
    private final List<ClipboardEntity> entities = new ArrayList<>();

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

    /**
     * Create a new instance.
     *
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

    public BlockArrayClipboard(Region region, FaweClipboard clipboard) {
        checkNotNull(region);
        this.region = region.clone();
        this.size = getDimensions();
        this.origin = region.getMinimumPoint();
        this.IMP = clipboard;
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
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
    protected void finalize() {
        close();
    }

    @Override
    public void close() {
        IMP.close();

    }

    @Override
    public Region getRegion() {
        return region;
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
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : getEntities()) {
            if (region.contains(entity.getLocation().toVector().toBlockPoint())) {
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
            int x = position.getBlockX() - mx;
            int y = position.getBlockY() - my;
            int z = position.getBlockZ() - mz;
            return IMP.getBlock(x, y, z).toImmutableState();
        }

        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if(region.contains(position)) {
            int x = position.getBlockX() - mx;
            int y = position.getBlockY() - my;
            int z = position.getBlockZ() - mz;
            return IMP.getBlock(x, y, z);
        }
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        if (region.contains(position)) {
            final int x = position.getBlockX();
            final int y = position.getBlockY();
            final int z = position.getBlockZ();
            return setBlock(x, y, z, block);
        }
        return false;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        x -= mx;
        y -= my;
        z -= mz;
        IMP.setTile(x, y, z, tag);
    }

    public void setTile(BlockVector3 position, CompoundTag tag) {
        setTile(position.getX(), position.getY(), position.getZ(), tag);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        x -= mx;
        y -= my;
        z -= mz;
        return IMP.setBlock(x, y, z, block);
    }

    @Override
    public boolean hasBiomes() {
        return IMP.hasBiomes();
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        BlockVector2 v = position.subtract(region.getMinimumPoint().toBlockVector2());
        return IMP.getBiome(v.getX(), v.getZ());
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        int x = position.getBlockX() - mx;
        int z = position.getBlockZ() - mz;
        return IMP.setBiome(x, z, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return IMP.setBiome(x, z, biome);
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }
}
