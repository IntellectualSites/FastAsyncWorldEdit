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

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.Order;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.UUID;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Specifies an object that implements something suitable as a "clipboard."
 */
public interface Clipboard extends Extent, Iterable<BlockVector3>, Closeable {

    static Clipboard create(Region region) {
        checkNotNull(region);
        checkNotNull(region.getWorld(),
            "World cannot be null (use the other constructor for the region)");
        EditSession session = new EditSessionBuilder(region.getWorld()).allowedRegionsEverywhere()
            .autoQueue(false).build();
        return ReadOnlyClipboard.of(session, region);
    }

    static Clipboard create(BlockVector3 size, UUID uuid) {
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            return new DiskOptimizedClipboard(size, uuid);
        } else if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL == 0) {
            return new CPUOptimizedClipboard(size);
        } else {
            return new MemoryOptimizedClipboard(size);
        }
    }

    /**
     * Get the bounding region of this extent.
     *
     * <p>Implementations should return a copy of the region.</p>
     *
     * @return the bounding region
     */
    Region getRegion();

    /**
     * Get the dimensions of the copy, which is at minimum (1, 1, 1).
     *
     * @return the dimensions
     */
    BlockVector3 getDimensions();

    /**
     * Get the origin point from which the copy was made from.
     *
     * @return the origin
     */
    BlockVector3 getOrigin();

    /**
     * Set the origin point from which the copy was made from.
     *
     * @param origin the origin
     */
    void setOrigin(BlockVector3 origin);

    /**
     * Returns true if the clipboard has biome data. This can be checked since {@link
     * Extent#getBiome(BlockVector2)} strongly suggests returning {@link
     * com.sk89q.worldedit.world.biome.BiomeTypes#OCEAN} instead of {@code null} if biomes aren't
     * present. However, it might not be desired to set areas to ocean if the clipboard is
     * defaulting to ocean, instead of having biomes explicitly set.
     *
     * @return true if the clipboard has biome data set
     */
    default boolean hasBiomes() {
        return false;
    }

    /**
     * Remove entity from clipboard
     */
    void removeEntity(Entity entity);

    default int getWidth() {
        return getDimensions().getBlockX();
    }

    default int getHeight() {
        return getDimensions().getBlockY();
    }

    default int getLength() {
        return getDimensions().getBlockZ();
    }

    default int getArea() {
        return getWidth() * getLength();
    }

    default int getVolume() {
        return getWidth() * getHeight() * getLength();
    }

    default Iterator<BlockVector3> iterator(Order order) {
        return order.create(getRegion());
    }

    @Override
    @NotNull
    default Iterator<BlockVector3> iterator() {
        return getRegion().iterator();
    }

    default Iterator<BlockVector2> iterator2d() {
        return Regions.asFlatRegion(getRegion()).asFlatRegion().iterator();
    }

    default URI getURI() {
        return null;
    }

    @Override
    default <T extends Filter> T apply(Region region, T filter, boolean full) {
        if (region.equals(getRegion())) {
            return apply(this, filter);
        }
        return apply(region, filter);
    }

    @Override
    default void close() {
    }

    /**
     * Forwards to paste(world, to, true, true, null)
     */
    default EditSession paste(World world, BlockVector3 to) {
        return paste(world, to, true, true, null);
    }

    default void save(File file, ClipboardFormat format) throws IOException {
        checkNotNull(file);
        checkNotNull(format);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            file.createNewFile();
        }
        save(new FileOutputStream(file), format);
    }

    /**
     * Save this schematic to a stream
     */
    default void save(OutputStream stream, ClipboardFormat format) throws IOException {
        checkNotNull(stream);
        checkNotNull(format);
        try (ClipboardWriter writer = format.getWriter(stream)) {
            writer.write(this);
        }
    }

    default EditSession paste(World world, BlockVector3 to, boolean allowUndo, boolean pasteAir,
        @Nullable Transform transform) {
        return paste(world, to, allowUndo, pasteAir, true, transform);
    }

    /**
     * Paste this schematic in a world
     */
    default EditSession paste(World world, BlockVector3 to, boolean allowUndo, boolean pasteAir,
        boolean copyEntities, @Nullable Transform transform) {
        checkNotNull(world);
        checkNotNull(to);
        EditSession editSession;
        if (world instanceof EditSession) {
            editSession = (EditSession) world;
        } else {
            EditSessionBuilder builder = new EditSessionBuilder(world).autoQueue(true)
                .checkMemory(false).allowedRegionsEverywhere().limitUnlimited();
            if (allowUndo) {
                editSession = builder.build();
            } else {
                editSession = builder.changeSetNull().fastmode(true).build();
            }
        }
        Extent extent = this;
        Mask sourceMask = editSession.getSourceMask();
        if (transform != null && !transform.isIdentity()) {
            extent = new BlockTransformExtent(this, transform);
        } else if (sourceMask == null) {
            paste(editSession, to, pasteAir);
            editSession.flushQueue();
            return editSession;
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, this.getRegion(),
            this.getOrigin(), editSession, to);
        if (transform != null && !transform.isIdentity()) {
            copy.setTransform(transform);
        }
        copy.setCopyingEntities(copyEntities);
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(extent);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (!pasteAir) {
            copy.setSourceMask(new ExistingBlockMask(this));
        }
        try {
            Operations.completeLegacy(copy);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
        editSession.flushQueue();
        return editSession;
    }

    default void paste(Extent extent, BlockVector3 to, boolean pasteAir,
        @Nullable Transform transform) {
        Extent source = this;
        if (transform != null && !transform.isIdentity()) {
            source = new BlockTransformExtent(this, transform);
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(source, this.getRegion(), this.getOrigin(),
            extent, to);
        if (transform != null) {
            copy.setTransform(transform);
        }
        copy.setCopyingBiomes(this.hasBiomes());
        if (extent instanceof EditSession) {
            EditSession editSession = (EditSession) extent;
            Mask sourceMask = editSession.getSourceMask();
            if (sourceMask != null) {
                new MaskTraverser(sourceMask).reset(extent);
                copy.setSourceMask(sourceMask);
                editSession.setSourceMask(null);
            }
        }
        if (!pasteAir) {
            copy.setSourceMask(new ExistingBlockMask(this));
        }
        Operations.completeBlindly(copy);
    }

    default void paste(Extent extent, BlockVector3 to, boolean pasteAir) {
        final BlockVector3 origin = this.getOrigin();

        final boolean copyBiomes = this.hasBiomes();
        // To must be relative to the clipboard origin ( player location - clipboard origin ) (as the locations supplied are relative to the world origin)
        final int relx = to.getBlockX() - origin.getBlockX();
        final int rely = to.getBlockY() - origin.getBlockY();
        final int relz = to.getBlockZ() - origin.getBlockZ();

        MutableBlockVector2 mpos2d = new MutableBlockVector2();
        mpos2d.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (BlockVector3 pos : this) {
            BaseBlock block = pos.getFullBlock(this);
            int xx = pos.getX() + relx;
            int zz = pos.getZ() + relz;
            if (copyBiomes && xx != mpos2d.getBlockX() && zz != mpos2d.getBlockZ()) {
                mpos2d.setComponents(xx, zz);
                extent.setBiome(mpos2d, Clipboard.this.getBiome(pos.toBlockVector2()));
            }
            if (!pasteAir && block.getBlockType().getMaterial().isAir()) {
                continue;
            }
            if (pos.getY() < 0) {
                throw new RuntimeException("Y-Position cannot be less than 0!");
            }
            extent.setBlock(xx, pos.getY() + rely, zz, block);
        }
        // Entity offset is the paste location subtract the clipboard origin (entity's location is already relative to the world origin)
        final int entityOffsetX = to.getBlockX() - origin.getBlockX();
        final int entityOffsetY = to.getBlockY() - origin.getBlockY();
        final int entityOffsetZ = to.getBlockZ() - origin.getBlockZ();
        // entities
        for (Entity entity : this.getEntities()) {
            // skip players on pasting schematic
            if (entity.getState() != null && entity.getState().getType().getId()
                .equals("minecraft:player")) {
                continue;
            }
            Location pos = entity.getLocation();
            Location newPos = new Location(pos.getExtent(), pos.getX() + entityOffsetX,
                pos.getY() + entityOffsetY, pos.getZ() + entityOffsetZ, pos.getYaw(),
                pos.getPitch());
            extent.createEntity(newPos, entity.getState());
        }
    }
}
