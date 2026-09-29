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

import com.fastasyncworldedit.core.extent.clipboard.SimpleClipboard;
import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.math.MutableBlockVector2;
import com.fastasyncworldedit.core.math.OffsetBlockVector3;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.google.common.collect.Iterators;
import com.sk89q.jnbt.CompoundTag;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores block data as a multi-dimensional array of {@link BlockState}s and
 * other data as lists or maps. Clipboard may need to be flushed before use
 * if using clipboard-on-disk.
 */
public class BlockArrayClipboard implements Clipboard {

    //FAWE start
    private final Region region;
    private final Clipboard parent;
    private final BlockVector3 offset;
    private BlockVector3 origin;

    /**
     * Create a new instance. Creates a parent clipboard based on the clipboard settings in settings.yml, with a randomly
     * generated {@link UUID} ID.
     * Depending on settings, parent will be on of:
     * {@link com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard}
     * <p>
     * If using clipboard-on-disk, the clipboard should be flushed {@link Clipboard#flush()} before use.
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region) {
        this(region, UUID.randomUUID());
    }

    /**
     * Create a new instance, storage-backed by the given clipboard. Clipboard must be of one of the following types:
     * {@link com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard}
     * <p>
     * Do not use this constructor if you do not know what you are doing. See
     * {@link BlockArrayClipboard#BlockArrayClipboard(Region)} or {@link BlockArrayClipboard#BlockArrayClipboard(Region, UUID)}
     *
     * @param parent storage clipboard
     * @param offset offset of clipboard region from origin
     */
    public BlockArrayClipboard(SimpleClipboard parent, BlockVector3 offset) {
        this.parent = parent;
        Region shifted = parent.getRegion().clone();
        shifted.shift(offset);
        this.region = shifted;
        this.offset = shifted.getMinimumPoint();
        this.origin = parent.getOrigin().add(this.offset);
    }

    /**
     * Create a new instance. Creates a parent clipboard based on the clipboard settings in settings.yml.
     * Depending on settings, parent will be on of:
     * {@link com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard}
     * <p>
     * If using clipboard-on-disk, the clipboard should be flushed ({@link Clipboard#flush()}) before use.
     *
     * @param region      the bounding region
     * @param clipboardId clipboard ID
     */
    public BlockArrayClipboard(Region region, UUID clipboardId) {
        this(region, (SimpleClipboard) Clipboard.create(region, clipboardId));
    }

    /**
     * Create a new instance, storage-backed by the given clipboard. Clipboard must be of one of the following types:
     * {@link com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard}
     * {@link com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard}
     * <p>
     * Do not use this constructor if you do not know what you are doing. See
     * {@link BlockArrayClipboard#BlockArrayClipboard(Region)} or {@link BlockArrayClipboard#BlockArrayClipboard(Region, UUID)}
     *
     * @param region the bounding region
     * @param parent storage clipboard
     */
    public BlockArrayClipboard(Region region, SimpleClipboard parent) {
        checkNotNull(parent);
        checkNotNull(region);
        this.parent = parent;
        this.region = region.clone();
        this.offset = region.getMinimumPoint();
        this.origin = parent.getOrigin();
    }
    //FAWE end

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        this.origin = origin;
        getParent().setOrigin(origin);
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
            int x = position.x() - offset.x();
            int y = position.y() - offset.y();
            int z = position.z() - offset.z();
            return getParent().getBlock(x, y, z);
        }

        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (region.contains(position)) {
            int x = position.x() - offset.x();
            int y = position.y() - offset.y();
            int z = position.z() - offset.z();
            return getParent().getFullBlock(x, y, z);
        }
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) {
        if (region.contains(position)) {
            //FAWE - get points
            final int x = position.x();
            final int y = position.y();
            final int z = position.z();
            return setBlock(x, y, z, block);
            //FAWE end
        }
        return false;
    }

    //FAWE start
    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tag) {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        return getParent().tile(x, y, z, tag);
    }


    @Deprecated(forRemoval = true, since = "2.11.2")
    public boolean setTile(BlockVector3 position, CompoundTag tag) {
        return tile(position.x(), position.y(), position.z(), FaweCompoundTag.of(tag.toLinTag()));
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        return parent.setBlock(x, y, z, block);
    }

    @Override
    public boolean hasBiomes() {
        return getParent().hasBiomes();
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        if (!region.contains(position)) {
            return null;
        }
        int x = position.x() - offset.x();
        int y = position.y() - offset.y();
        int z = position.z() - offset.z();
        return getParent().getBiomeType(x, y, z);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        int x = position.x() - offset.x();
        int y = position.y() - offset.y();
        int z = position.z() - offset.z();
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        region = region.clone();
        region.shift(BlockVector3.ZERO.subtract(offset));
        return getParent().getEntities(region).stream().map(e ->
        {
            if (e instanceof ClipboardEntity) {
                ClipboardEntity ce = (ClipboardEntity) e;
                Location oldloc = ce.getLocation();
                Location loc = new Location(oldloc.getExtent(),
                        oldloc.x() + offset.x(),
                        oldloc.y() + offset.y(),
                        oldloc.z() + offset.z(),
                        oldloc.getYaw(), oldloc.getPitch()
                );
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
                        oldloc.x() + offset.x(),
                        oldloc.y() + offset.y(),
                        oldloc.z() + offset.z(),
                        oldloc.getYaw(), oldloc.getPitch()
                );
                return new ClipboardEntity(loc, ce.entity);
            }
            return e;
        }).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        Location l = new Location(location.getExtent(),
                location.x() - offset.x(),
                location.y() - offset.y(),
                location.z() - offset.z(),
                location.getYaw(), location.getPitch()
        );
        return getParent().createEntity(l, entity);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        Location l = new Location(location.getExtent(),
                location.x() - offset.x(),
                location.y() - offset.y(),
                location.z() - offset.z(),
                location.getYaw(), location.getPitch()
        );
        return getParent().createEntity(l, entity, uuid);
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        getParent().removeEntity(x, y, z, uuid);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        return getParent().getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        return getParent().getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        x -= offset.x();
        y -= offset.y();
        z -= offset.z();
        return getParent().getBiomeType(x, y, z);
    }

    @Nonnull
    @Override
    public Iterator<BlockVector3> iterator() {
        OffsetBlockVector3 mutable = new OffsetBlockVector3(offset);
        return Iterators.transform(getParent().iterator(), mutable::init);
    }

    @Override
    public Iterator<BlockVector2> iterator2d() {
        MutableBlockVector2 mutable = new MutableBlockVector2();
        return Iterators.transform(getParent().iterator2d(), input ->
                mutable.setComponents(input.x() + offset.x(), input.z() + offset.z()));
    }

    @Override
    public Iterator<BlockVector3> iterator(Order order) {
        OffsetBlockVector3 mutable = new OffsetBlockVector3(offset);
        return Iterators.transform(getParent().iterator(order), mutable::init);
    }
    //FAWE end

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

    @Override
    public void flush() {
        this.parent.flush();
    }

    //FAWE start

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
            this((Clipboard) loc.getExtent(), loc.x(), loc.y(), loc.z(), loc.getYaw(), loc.getPitch(), entity);
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
    //FAWE end
}
