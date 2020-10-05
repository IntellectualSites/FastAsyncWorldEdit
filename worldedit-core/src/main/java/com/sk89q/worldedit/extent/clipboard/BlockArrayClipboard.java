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

package com.sk89q.worldedit.extent.clipboard;

import com.google.common.collect.Iterators;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.visitor.Order;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.OffsetBlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores block data as a multi-dimensional array of {@link BlockState}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard implements Clipboard {

    private final Region region;
    private final BlockVector3 origin;
    private final Clipboard parent;

    public BlockArrayClipboard(Region region) {
        this(region, UUID.randomUUID());
    }

    public BlockArrayClipboard(Clipboard clipboard, BlockVector3 offset) {
        this.parent = clipboard;
        Region shifted = clipboard.getRegion().clone();
        shifted.shift(offset);
        this.region = shifted;
        this.origin = shifted.getMinimumPoint();
    }

    /**
     * Create a new instance.
     *
     * <p>The origin will be placed at the region's lowest minimum point.</p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region, UUID clipboardId) {
        this(region, Clipboard.create(region, clipboardId));
    }

    public BlockArrayClipboard(Region region, Clipboard parent) {
        checkNotNull(parent);
        checkNotNull(region);
        this.parent = parent;
        this.region = region.clone();
        this.origin = region.getMinimumPoint();
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public BlockVector3 getOrigin() {
        return getParent().getOrigin().add(region.getMinimumPoint());
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        getParent().setOrigin(origin.subtract(region.getMinimumPoint()));
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
    public BlockState getBlock(BlockVector3 position) {
        if (region.contains(position)) {
            int x = position.getBlockX() - origin.getX();
            int y = position.getBlockY() - origin.getY();
            int z = position.getBlockZ() - origin.getZ();
            return getParent().getBlock(x, y, z);
        }

        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (region.contains(position)) {
            int x = position.getBlockX() - origin.getX();
            int y = position.getBlockY() - origin.getY();
            int z = position.getBlockZ() - origin.getZ();
            return getParent().getFullBlock(x, y, z);
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
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return getParent().setTile(x, y, z, tag);
    }

    public boolean setTile(BlockVector3 position, CompoundTag tag) {
        return setTile(position.getX(), position.getY(), position.getZ(), tag);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return parent.setBlock(x, y, z, block);
    }

    @Override
    public boolean hasBiomes() {
        return getParent().hasBiomes();
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        BlockVector3 v = position.subtract(region.getMinimumPoint());
        return getParent().getBiomeType(v.getX(), v.getY(), v.getZ());
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        int x = position.getBlockX() - origin.getX();
        int y = position.getBlockY() - origin.getY();
        int z = position.getBlockZ() - origin.getZ();
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        region = region.clone();
        region.shift(BlockVector3.ZERO.subtract(origin));
        return getParent().getEntities(region).stream().map(e ->
        {
            if (e instanceof ClipboardEntity) {
                ClipboardEntity ce = (ClipboardEntity) e;
                Location oldloc = ce.getLocation();
                Location loc = new Location(oldloc.getExtent(),
                                            oldloc.getX() + origin.getBlockX(),
                                            oldloc.getY() + origin.getBlockY(),
                                            oldloc.getZ() + origin.getBlockZ(),
                                            oldloc.getYaw(), oldloc.getPitch());
                return new ClipboardEntity(loc, ce.entity);
            }
            return e;
        }).collect(Collectors.toList());
    }

    @Override
    public List<? extends Entity> getEntities() {
        return getParent().getEntities().stream().map(e ->
        {
            if (e instanceof ClipboardEntity) {
                ClipboardEntity ce = (ClipboardEntity) e;
                Location oldloc = ce.getLocation();
                Location loc = new Location(oldloc.getExtent(),
                                            oldloc.getX() + origin.getBlockX(),
                                            oldloc.getY() + origin.getBlockY(),
                                            oldloc.getZ() + origin.getBlockZ(),
                                            oldloc.getYaw(), oldloc.getPitch());
                return new ClipboardEntity(loc, ce.entity);
            }
            return e;
        }).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        Location l = new Location(location.getExtent(),
                                  location.getX() - origin.getBlockX(),
                                  location.getY() - origin.getBlockY(),
                                  location.getZ() - origin.getBlockZ(),
                                  location.getYaw(), location.getPitch());
        return getParent().createEntity(l, entity);
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        getParent().removeEntity(x, y, z, uuid);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return getParent().getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return getParent().getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        x -= origin.getX();
        z -= origin.getZ();
        return getParent().getBiomeType(x, y, z);
    }

    @NotNull
    @Override
    public Iterator<BlockVector3> iterator() {
        OffsetBlockVector3 mutable = new OffsetBlockVector3(origin);
        return Iterators.transform(getParent().iterator(), mutable::init);
    }

    @Override
    public Iterator<BlockVector2> iterator2d() {
        MutableBlockVector2 mutable = new MutableBlockVector2();
        return Iterators.transform(getParent().iterator2d(), input ->
        mutable.setComponents(input.getX() + origin.getX(), input.getZ() + origin.getZ()));
    }

    @Override
    public Iterator<BlockVector3> iterator(Order order) {
        OffsetBlockVector3 mutable = new OffsetBlockVector3(origin);
        return Iterators.transform(getParent().iterator(order), mutable::init);
    }

    @Override
    public BlockVector3 getDimensions() {
        return this.parent.getDimensions();
    }

    @Override
    public void removeEntity(Entity entity) {
        this.parent.removeEntity(entity);
    }

    public Clipboard getParent() {
        return parent;
    }

    @Override
    public void close() {
        this.parent.close();
    }

    /**
     * Stores entity data.
     */
    public static class ClipboardEntity implements Entity {
        private final BaseEntity entity;
        private final Clipboard clipboard;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public ClipboardEntity(Location loc, BaseEntity entity) {
            this((Clipboard) loc.getExtent(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), entity);
        }

        public ClipboardEntity(Clipboard clipboard, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
            checkNotNull(entity);
            checkNotNull(clipboard);
            this.clipboard = clipboard;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.entity = new BaseEntity(entity);
        }

        @Override
        public boolean remove() {
            clipboard.removeEntity(this);
            return true;
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }

        /**
         * Get the entity state. This is not a copy.
         *
         * @return the entity
         */
        BaseEntity getEntity() {
            return entity;
        }

        @Override
        public BaseEntity getState() {
            return new BaseEntity(entity);
        }

        @Override
        public Location getLocation() {
            return new Location(clipboard, x, y, z, yaw, pitch);
        }

        @Override
        public Extent getExtent() {
            return clipboard;
        }

        @Override
        public boolean setLocation(Location loc) {
            clipboard.removeEntity(this);
            Entity result = clipboard.createEntity(loc, entity);
            return result != null;
        }
    }
}
