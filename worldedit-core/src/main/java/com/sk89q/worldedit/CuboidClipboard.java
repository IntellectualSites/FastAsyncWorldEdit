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

package com.sk89q.worldedit;

import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.MainUtil;

import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.FlattenedClipboardTransform;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The clipboard remembers the state of a cuboid region.
 *
 * @deprecated This is slowly being replaced with {@link Clipboard}, which is
 * far more versatile. Transforms are supported using affine
 * transformations and full entity support is provided because
 * the clipboard properly implements {@link Extent}. However,
 * the new clipboard class is only available in WorldEdit 6.x and
 * beyond. We intend on keeping this deprecated class in WorldEdit
 * for an extended amount of time so there is no rush to
 * switch (but new features will not be supported). To copy between
 * a clipboard and a world (or between any two {@code Extent}s),
 * one can use {@link ForwardExtentCopy}. See
 * {@link ClipboardCommands} and {@link SchematicCommands} for
 * more information.
 */
@Deprecated
public class CuboidClipboard {

    /**
     * An enum of possible flip directions.
     */
    public enum FlipDirection {
        NORTH_SOUTH(Direction.NORTH),
        WEST_EAST(Direction.WEST),
        UP_DOWN(Direction.UP),
        ;
        private final Direction direction;
        FlipDirection(Direction direction) {
            this.direction = direction;
        }
    }

    private BlockArrayClipboard clipboard;
    private AffineTransform transform;
    public BlockVector3 size;

    /**
     * Constructs the clipboard.
     *
     * @param size the dimensions of the clipboard (should be at least 1 on every dimension)
     */
    public CuboidClipboard(BlockVector3 size) {
        checkNotNull(size);
        MainUtil.warnDeprecated(BlockArrayClipboard.class, ClipboardFormat.class);
        this.size = size;
        this.clipboard = this.init(BlockVector3.ZERO, BlockVector3.ZERO);
    }

    public CuboidClipboard(BlockArrayClipboard clipboard) {
        this.clipboard = clipboard;
        this.size = clipboard.getDimensions();
    }

    /**
     * Constructs the clipboard.
     *
     * @param size   the dimensions of the clipboard (should be at least 1 on every dimension)
     * @param origin the origin point where the copy was made, which must be the
     *               {@link CuboidRegion#getMinimumPoint()} relative to the copy
     */
    public CuboidClipboard(BlockVector3 size, BlockVector3 origin) {
        checkNotNull(size);
        checkNotNull(origin);
        MainUtil.warnDeprecated(BlockArrayClipboard.class, ClipboardFormat.class);
        this.size = size;
        this.clipboard = init(BlockVector3.ZERO, origin);
    }

    /**
     * Constructs the clipboard.
     *
     * @param size   the dimensions of the clipboard (should be at least 1 on every dimension)
     * @param origin the origin point where the copy was made, which must be the
     *               {@link CuboidRegion#getMinimumPoint()} relative to the copy
     * @param offset the offset from the minimum point of the copy where the user was
     */
    public CuboidClipboard(BlockVector3 size, BlockVector3 origin, BlockVector3 offset) {
        checkNotNull(size);
        checkNotNull(origin);
        checkNotNull(offset);
        MainUtil.warnDeprecated(BlockArrayClipboard.class, ClipboardFormat.class);
        this.size = size;
        this.clipboard = this.init(offset, origin);
    }

    /* ------------------------------------------------------------------------------------------------------------- */

    private BlockArrayClipboard init(BlockVector3 offset, BlockVector3 min) {
    	BlockVector3 origin = min.subtract(offset);
        CuboidRegion  region = new CuboidRegion(min, min.add(size).subtract(BlockVector3.ONE));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(origin);
        return clipboard;
    }

    /* ------------------------------------------------------------------------------------------------------------- */

    public BaseBlock getBlock(BlockVector3 position) {
        return getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }


    public BaseBlock getBlock(int x, int y, int z) {
//        return adapt(clipboard.IMP.getBlock(x, y, z));
    	return clipboard.IMP.getBlock(x, y, z);
    }

    public BaseBlock getLazyBlock(BlockVector3 position) {
        return getBlock(position);
    }

    public void setBlock(BlockVector3 location, BaseBlock block) {
        setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        return setBlock(x, y, z, block);
    }

    public boolean setBlock(int x, int y, int z, BlockState block) {
        return clipboard.IMP.setBlock(x, y, z, block);
    }

    /**
     * Get the width (X-direction) of the clipboard.
     *
     * @return width
     */
    public int getWidth() {
        return size.getBlockX();
    }

    /**
     * Get the length (Z-direction) of the clipboard.
     *
     * @return length
     */
    public int getLength() {
        return size.getBlockZ();
    }

    /**
     * Get the height (Y-direction) of the clipboard.
     *
     * @return height
     */
    public int getHeight() {
        return size.getBlockY();
    }

    /**
     * Rotate the clipboard in 2D. It can only rotate by angles divisible by 90.
     *
     * @param angle in degrees
     */
    @SuppressWarnings("deprecation")
    public void rotate2D(int angle) {
        AffineTransform newTransform = new AffineTransform().rotateY(-angle);
        this.transform = transform == null ? newTransform : newTransform.combine(transform);
    }

    /**
     * Flip the clipboard.
     *
     * @param dir direction to flip
     */
    public void flip(FlipDirection dir) {
        flip(dir, false);
    }

    /**
     * Flip the clipboard.
     *
     * @param dir          direction to flip
     * @param aroundPlayer flip the offset around the player
     */
    @SuppressWarnings("deprecation")
    public void flip(FlipDirection dir, boolean aroundPlayer) {
        checkNotNull(dir);
        Direction direction = dir.direction;
        AffineTransform newTransform = new AffineTransform().scale(direction.toVector().abs().multiply(-2).add(1, 1, 1));
        this.transform = transform == null ? newTransform : newTransform.combine(transform);
    }

    /**
     * Copies blocks to the clipboard.
     *
     * @param editSession the EditSession from which to take the blocks
     */
    public void copy(EditSession editSession) {
        for (int x = 0; x < size.getBlockX(); ++x) {
            for (int y = 0; y < size.getBlockY(); ++y) {
                for (int z = 0; z < size.getBlockZ(); ++z) {
                    setBlock(x, y, z, editSession.getBlock(BlockVector3.at(x, y, z).add(getOrigin())));
                }
            }
        }
    }

    /**
     * Copies blocks to the clipboard.
     *
     * @param editSession The EditSession from which to take the blocks
     * @param region      A region that further constrains which blocks to take.
     */
    public void copy(EditSession editSession, Region region) {
        for (int x = 0; x < size.getBlockX(); ++x) {
            for (int y = 0; y < size.getBlockY(); ++y) {
                for (int z = 0; z < size.getBlockZ(); ++z) {
                    final BlockVector3 pt = BlockVector3.at(x, y, z).add(getOrigin());
                    if (region.contains(pt)) {
                        setBlock(x, y, z, editSession.getBlock(pt));
                    } else {
                        setBlock(x, y, z, (BlockState)null);
                    }
                }
            }
        }
    }

    /**
     * Paste the clipboard at the given location using the given {@code EditSession}.
     * <p>
     * <p>This method blocks the server/game until the entire clipboard is
     * pasted. In the future, {@link ForwardExtentCopy} will be recommended,
     * which, if combined with the proposed operation scheduler framework,
     * will not freeze the game/server.</p>
     *
     * @param editSession the EditSession to which blocks are to be copied to
     * @param newOrigin   the new origin point (must correspond to the minimum point of the cuboid)
     * @param noAir       true to not copy air blocks in the source
     * @throws MaxChangedBlocksException thrown if too many blocks were changed
     */
    public void paste(EditSession editSession, BlockVector3 newOrigin, boolean noAir) throws MaxChangedBlocksException {
        paste(editSession, newOrigin, noAir, false);
    }

    /**
     * Paste the clipboard at the given location using the given {@code EditSession}.
     * <p>
     * <p>This method blocks the server/game until the entire clipboard is
     * pasted. In the future, {@link ForwardExtentCopy} will be recommended,
     * which, if combined with the proposed operation scheduler framework,
     * will not freeze the game/server.</p>
     *
     * @param editSession the EditSession to which blocks are to be copied to
     * @param newOrigin   the new origin point (must correspond to the minimum point of the cuboid)
     * @param noAir       true to not copy air blocks in the source
     * @param entities    true to copy entities
     * @throws MaxChangedBlocksException thrown if too many blocks were changed
     */
    public void paste(EditSession editSession, BlockVector3 newOrigin, boolean noAir, boolean entities) throws MaxChangedBlocksException {
        new Schematic(clipboard).paste(editSession, newOrigin, false, !noAir, entities, transform);
        editSession.flushQueue();
    }

    /**
     * Paste the clipboard at the given location using the given {@code EditSession}.
     * <p>
     * <p>This method blocks the server/game until the entire clipboard is
     * pasted. In the future, {@link ForwardExtentCopy} will be recommended,
     * which, if combined with the proposed operation scheduler framework,
     * will not freeze the game/server.</p>
     *
     * @param editSession the EditSession to which blocks are to be copied to
     * @param newOrigin   the new origin point (must correspond to the minimum point of the cuboid)
     * @param noAir       true to not copy air blocks in the source
     * @throws MaxChangedBlocksException thrown if too many blocks were changed
     */
    public void place(EditSession editSession, BlockVector3 newOrigin, boolean noAir) throws MaxChangedBlocksException {
        paste(editSession, newOrigin, noAir, false);
    }

    /**
     * Get the block at the given position.
     * <p>
     * <p>If the position is out of bounds, air will be returned.</p>
     *
     * @param position the point, relative to the origin of the copy (0, 0, 0) and not to the actual copy origin
     * @return air, if this block was outside the (non-cuboid) selection while copying
     * @throws ArrayIndexOutOfBoundsException if the position is outside the bounds of the CuboidClipboard
     * @deprecated use {@link #getBlock(Vector)} instead
     */
    @Deprecated
    public BaseBlock getPoint(BlockVector3 position) throws ArrayIndexOutOfBoundsException {
        final BaseBlock block = getBlock(position);
        if (block == null) {
            return new BaseBlock(BlockTypes.AIR);
        }

        return block;
    }

    /**
     * Get the origin point, which corresponds to where the copy was
     * originally copied from. The origin is the lowest possible X, Y, and
     * Z components of the cuboid region that was copied.
     *
     * @return the origin
     */
    public BlockVector3 getOrigin() {
        return clipboard.getMinimumPoint();
    }

    /**
     * Set the origin point, which corresponds to where the copy was
     * originally copied from. The origin is the lowest possible X, Y, and
     * Z components of the cuboid region that was copied.
     *
     * @param origin the origin to set
     */
    public void setOrigin(BlockVector3 origin) {
        checkNotNull(origin);
        setOriginAndOffset(getOffset(), origin);
    }

    public void setOriginAndOffset(BlockVector3 offset, BlockVector3 min) {
    	BlockVector3 origin = min.subtract(offset);
        CuboidRegion  region = new CuboidRegion(min, min.add(size).subtract(BlockVector3.ONE));
        clipboard.setRegion(region);
        clipboard.setOrigin(origin);
    }

    /**
     * Get the offset of the player to the clipboard's minimum point
     * (minimum X, Y, Z coordinates).
     * <p>
     * <p>The offset is inverse (multiplied by -1).</p>
     *
     * @return the offset the offset
     */
    public BlockVector3 getOffset() {
    	BlockVector3 min = clipboard.getMinimumPoint();
    	BlockVector3 origin = clipboard.getOrigin();
    	BlockVector3 offset = min.subtract(origin);
        return offset;
    }

    /**
     * Set the offset of the player to the clipboard's minimum point
     * (minimum X, Y, Z coordinates).
     * <p>
     * <p>The offset is inverse (multiplied by -1).</p>
     *
     * @param offset the new offset
     */
    public void setOffset(BlockVector3 offset) {
        checkNotNull(offset);
        setOriginAndOffset(offset, getOrigin());
    }

    /**
     * Get the dimensions of the clipboard.
     *
     * @return the dimensions, where (1, 1, 1) is 1 wide, 1 across, 1 deep
     */
    public BlockVector3 getSize() {
        return size;
    }

    /**
     * Saves the clipboard data to a .schematic-format file.
     *
     * @param path the path to the file to save
     * @throws IOException   thrown on I/O error
     * @throws DataException thrown on error writing the data for other reasons
     * @deprecated use {@link ClipboardFormat#SCHEMATIC}
     */
    @Deprecated
    public void saveSchematic(File path) throws IOException, DataException {
        checkNotNull(path);
        if (transform != null && !transform.isIdentity()) {
            final FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform);
            BlockArrayClipboard target = new BlockArrayClipboard(result.getTransformedRegion(), UUID.randomUUID());
            target.setOrigin(clipboard.getOrigin());
            Operations.completeLegacy(result.copyTo(target));
            this.clipboard = target;
        }
        new Schematic(clipboard).save(path, BuiltInClipboardFormat.SPONGE_SCHEMATIC);
    }

    /**
     * Load a .schematic file into a clipboard.
     *
     * @param path the path to the file to load
     * @return a clipboard
     * @throws IOException   thrown on I/O error
     * @throws DataException thrown on error writing the data for other reasons
     * @deprecated use {@link ClipboardFormat#SCHEMATIC}
     */
    @Deprecated
    public static CuboidClipboard loadSchematic(File path) throws DataException, IOException {
        checkNotNull(path);
        return new CuboidClipboard((BlockVector3) BuiltInClipboardFormat.MCEDIT_SCHEMATIC.load(path).getClipboard());
    }

    /**
     * Get the block distribution inside a clipboard.
     *
     * @return a block distribution
     */
    public List<Countable<Integer>> getBlockDistribution() {
        List<Countable<Integer>> distribution = new ArrayList<>();
        List<Countable<BlockState>> distr = clipboard.getBlockDistributionWithData(clipboard.getRegion());
        for (Countable<BlockState> item : distr) {
            BlockStateHolder state = item.getID();
            int[] legacyId = LegacyMapper.getInstance().getLegacyFromBlock(state.toImmutableState());
            if (legacyId[0] != 0) distribution.add(new Countable<>(legacyId[0], item.getAmount()));
        }
        return distribution;
    }

    /**
     * Get the block distribution inside a clipboard with data values.
     *
     * @return a block distribution
     */
    public List<Countable<BaseBlock>> getBlockDistributionWithData() {
        List<Countable<BaseBlock>> distribution = new ArrayList<>();
        List<Countable<BlockState>> distr = clipboard.getBlockDistributionWithData(clipboard.getRegion());
        for (Countable<BlockState> item : distr) {
            distribution.add(new Countable<>(item.getID().toBaseBlock(), item.getAmount()));
        }
        return distribution;
    }
}