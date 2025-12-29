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

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard;
import com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard;
import com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard;
import com.fastasyncworldedit.core.extent.clipboard.ReadOnlyClipboard;
import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.util.MaskTraverser;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionBuilder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.internal.util.ClipboardTransformBaker;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.entity.EntityTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Specifies an object that implements something suitable as a "clipboard."
 */
//FAWE start - Iterable, closeable and flushable
public interface Clipboard extends Extent, Iterable<BlockVector3>, Closeable, Flushable {
    //FAWE end

    //FAWE start

    /**
     * Creates a new {@link ReadOnlyClipboard}.
     *
     * @deprecated Internal use only. Use {@link BlockArrayClipboard#BlockArrayClipboard(Region)}
     */
    @Deprecated
    static Clipboard create(Region region) {
        checkNotNull(region);
        checkNotNull(
                region.getWorld(),
                "World cannot be null (use the other constructor for the region)"
        );
        EditSession session = WorldEdit
                .getInstance()
                .newEditSessionBuilder()
                .world(region.getWorld())
                .allowedRegionsEverywhere()
                .build();
        return ReadOnlyClipboard.of(session, region);
    }

    /**
     * Create a new {@link com.fastasyncworldedit.core.extent.clipboard.SimpleClipboard} instance.
     * Will be one of the following, depending on settings:
     *  - {@link DiskOptimizedClipboard}
     *  - {@link CPUOptimizedClipboard}
     *  - {@link MemoryOptimizedClipboard}
     *
     * @deprecated Internal use only. Use {@link BlockArrayClipboard#BlockArrayClipboard(Region, UUID)}
     */
    @Deprecated
    static Clipboard create(Region region, UUID uuid) {
        if (Settings.settings().CLIPBOARD.USE_DISK) {
            return new DiskOptimizedClipboard(region, uuid);
        } else if (Settings.settings().CLIPBOARD.COMPRESSION_LEVEL == 0) {
            return new CPUOptimizedClipboard(region);
        } else {
            return new MemoryOptimizedClipboard(region);
        }
    }
    //FAWE end

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
     * Returns true if the clipboard has biome data. This can be checked since {@link Extent#getBiome(BlockVector3)}
     * strongly suggests returning {@link com.sk89q.worldedit.world.biome.BiomeTypes#OCEAN} instead of {@code null}
     * if biomes aren't present.
     *
     * @return true if the clipboard has biome data set
     */
    default boolean hasBiomes() {
        return false;
    }

    /**
     * Returns a clipboard with a given transform baked in.
     *
     * <p>
     * Note: This method may return the same clipboard object, if a copy is needed then you should check the returned value for identity equality and copy if needed.
     * </p>
     *
     * @param transform The transform
     * @return The new clipboard
     * @throws WorldEditException if the copy encounters an error
     */
    default Clipboard transform(Transform transform) throws WorldEditException {
        return ClipboardTransformBaker.bakeTransform(this, transform);
    }

    //FAWE start

    /**
     * Remove entity from clipboard.
     */
    void removeEntity(Entity entity);

    default int getWidth() {
        return getDimensions().x();
    }

    default int getHeight() {
        return getDimensions().y();
    }

    default int getLength() {
        return getDimensions().z();
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
    @Nonnull
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

    /**
     * Close the clipboard. May not allow further reading of the clipboard if saved on disk.
     */
    @Override
    default void close() {
    }

    /**
     * Flush the clipboard if appropriate. Only does something if using clipboard-on-disk. Blocking method and ensures all data
     * is saved to disk for any further operation with the clipboard.
     */
    @Override
    default void flush() {
    }

    /**
     * Forwards to {@link #paste(World, BlockVector3, boolean, boolean, Transform)}.
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
     * Save this schematic to a stream.
     */
    default void save(OutputStream stream, ClipboardFormat format) throws IOException {
        checkNotNull(stream);
        checkNotNull(format);
        try (ClipboardWriter writer = format.getWriter(stream)) {
            writer.write(this);
        }
    }

    default EditSession paste(
            World world, BlockVector3 to, boolean allowUndo, boolean pasteAir,
            @Nullable Transform transform
    ) {
        return paste(world, to, allowUndo, pasteAir, true, transform);
    }

    /**
     * Paste this schematic in a world.
     */
    default EditSession paste(
            World world, BlockVector3 to, boolean allowUndo, boolean pasteAir,
            boolean pasteEntities, @Nullable Transform transform
    ) {
        checkNotNull(world);
        checkNotNull(to);
        EditSession editSession;
        if (world instanceof EditSession) {
            editSession = (EditSession) world;
        } else {
            EditSessionBuilder builder = WorldEdit
                    .getInstance()
                    .newEditSessionBuilder()
                    .world(world)
                    .checkMemory(false)
                    .allowedRegionsEverywhere()
                    .limitUnlimited();
            if (allowUndo) {
                editSession = builder.build();
            } else {
                editSession = builder.changeSetNull().fastMode(true).build();
            }
        }
        Extent extent = this;
        Mask sourceMask = editSession.getSourceMask();
        if (transform != null && !transform.isIdentity()) {
            extent = new BlockTransformExtent(this, transform);
        } else if (sourceMask == null) {
            paste(editSession, to, pasteAir, pasteEntities, hasBiomes());
            editSession.flushQueue();
            return editSession;
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, this.getRegion(),
                this.getOrigin(), editSession, to
        );
        if (transform != null && !transform.isIdentity()) {
            copy.setTransform(transform);
        }
        copy.setCopyingEntities(pasteEntities);
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
        } finally {
            editSession.close(); // Make sure editsession is always closed
        }
        return editSession;
    }

    default void paste(
            Extent extent, BlockVector3 to, boolean pasteAir,
            @Nullable Transform transform
    ) {
        if (extent instanceof World) {
            EditSessionBuilder builder = WorldEdit
                    .getInstance()
                    .newEditSessionBuilder()
                    .world((World) extent)
                    .checkMemory(false)
                    .allowedRegionsEverywhere()
                    .limitUnlimited()
                    .changeSetNull();
            extent = builder.build();
        }

        Extent source = this;
        if (transform != null && !transform.isIdentity()) {
            source = new BlockTransformExtent(this, transform);
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(source, this.getRegion(), this.getOrigin(),
                extent, to
        );
        if (transform != null) {
            copy.setTransform(transform);
        }
        copy.setCopyingBiomes(this.hasBiomes());
        if (extent instanceof EditSession editSession) {
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
        paste(extent, to, pasteAir, false, false);
    }

    default void paste(Extent extent, BlockVector3 to, boolean pasteAir, boolean pasteEntities, boolean pasteBiomes) {
        boolean close = false;
        if (extent instanceof World) {
            close = true;
            EditSessionBuilder builder = WorldEdit
                    .getInstance()
                    .newEditSessionBuilder()
                    .world((World) extent)
                    .checkMemory(false)
                    .allowedRegionsEverywhere()
                    .limitUnlimited()
                    .changeSetNull();
            extent = builder.build();
        }

        final BlockVector3 origin = this.getOrigin();

        // To must be relative to the clipboard origin ( player location - clipboard origin ) (as the locations supplied are relative to the world origin)
        final int relx = to.x() - origin.x();
        final int rely = to.y() - origin.y();
        final int relz = to.z() - origin.z();

        pasteBiomes &= Clipboard.this.hasBiomes();

        for (BlockVector3 pos : this) {
            BaseBlock block = pos.getFullBlock(this);
            int xx = pos.x() + relx;
            int yy = pos.y() + rely;
            int zz = pos.z() + relz;
            if (pasteBiomes) {
                extent.setBiome(xx, yy, zz, pos.getBiome(this));
            }
            if (!pasteAir && block.getBlockType().getMaterial().isAir()) {
                continue;
            }
            extent.setBlock(xx, yy, zz, block);
        }
        // Entity offset is the paste location subtract the clipboard origin (entity's location is already relative to the world origin)
        final int entityOffsetX = to.x() - origin.x();
        final int entityOffsetY = to.y() - origin.y();
        final int entityOffsetZ = to.z() - origin.z();
        // entities
        if (pasteEntities) {
            for (Entity entity : this.getEntities()) {
                // skip players on pasting schematic
                if (entity.getState() != null && entity.getState().getType().id()
                        .equals("minecraft:player")) {
                    continue;
                }
                Location pos = entity.getLocation();
                Location newPos = new Location(pos.getExtent(), pos.x() + entityOffsetX,
                        pos.y() + entityOffsetY, pos.z() + entityOffsetZ, pos.getYaw(),
                        pos.getPitch()
                );
                extent.createEntity(newPos, entity.getState());
            }
        }
        if (close) {
            ((EditSession) extent).close();
        }
    }
    //FAWE end
}
