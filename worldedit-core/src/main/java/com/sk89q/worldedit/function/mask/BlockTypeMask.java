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

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mask that checks whether blocks at the given positions are matched by
 * a block in a list.
 *
 * <p>This mask checks for ONLY the block type. If state should also be checked,
 * use {@link BlockMask}.</p>
 * @deprecated use BlockMaskBuilder
 */
@Deprecated
public class BlockTypeMask extends AbstractExtentMask {

    private final boolean[] types;
    private boolean hasAir;

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param blocks a list of blocks to match
     */
    public BlockTypeMask(Extent extent, @NotNull Collection<BlockType> blocks) {
        this(extent, blocks.toArray(new BlockType[0]));
    }

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param block an array of blocks to match
     */
    public BlockTypeMask(Extent extent, @NotNull BlockType... block) {
        super(extent);
        this.types = new boolean[BlockTypes.size()];
        for (BlockType type : block) {
            add(type);
        }
    }

    private BlockTypeMask(Extent extent, boolean[] types, boolean hasAir) {
        super(extent);
        this.types = types;
        this.hasAir = hasAir;
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param blocks a list of blocks
     */
    public void add(@NotNull Collection<BlockType> blocks) {
        checkNotNull(blocks);
        for (BlockType type : blocks) {
            add(type);
        }
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param block an array of blocks
     */
    public void add(@NotNull BlockType... block) {
        for (BlockType type : block) {
            if (!hasAir && (type == BlockTypes.AIR || type == BlockTypes.CAVE_AIR || type == BlockTypes.VOID_AIR)) {
                hasAir = true;
            }
            this.types[type.getInternalId()] = true;
        }
    }

    /**
     * Get the list of blocks that are tested with.
     *
     * @return a list of blocks
     */
    public Collection<BlockType> getBlocks() {
        Set<BlockType> blocks = new HashSet<>();
        for (int i = 0; i < types.length; i++) {
            if (types[i]) blocks.add(BlockTypes.get(i));
        }
        return blocks;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(getExtent().getBlock(vector).getBlockType());
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        return test(extent.getBlock(vector).getBlockType());
    }

    @Override
    public boolean replacesAir() {
        return hasAir;
    }

    public boolean test(BlockType block) {
        return types[block.getInternalId()];
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

    @Override
    public Mask copy() {
        return new BlockTypeMask(getExtent(), types.clone(), hasAir);
    }

}
