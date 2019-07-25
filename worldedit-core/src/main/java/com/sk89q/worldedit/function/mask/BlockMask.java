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

package com.sk89q.worldedit.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * A mask that checks whether blocks at the given positions are matched by
 * a block in a list.
 *
 * <p>This mask checks for both an exact block type and state value match,
 * respecting fuzzy status of the BlockState.</p>
 */
public class BlockMask extends ABlockMask {

    private final boolean[] ordinals;

    public BlockMask() {
        this(new NullExtent());
    }

    public BlockMask(Extent extent) {
        this(extent, new boolean[BlockTypes.states.length]);
    }

    public BlockMask(Extent extent, boolean[] ordinals) {
        super(extent == null ? new NullExtent() : extent);
        this.ordinals = ordinals;
    }

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param blocks a list of blocks to match
     * @deprecated NBT not supported by this mask
     */
    @Deprecated
    public BlockMask(Extent extent, Collection<BaseBlock> blocks) {
        this(extent);
        add(blocks);
    }

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param block an array of blocks to match
     */
    public BlockMask(Extent extent, BaseBlock... block) {
        this(extent, Arrays.asList(checkNotNull(block)));
    }

    public BlockMask add(Predicate<BlockState> predicate) {
        for (int i = 0; i < ordinals.length; i++) {
            if (!ordinals[i]) {
                BlockState state = BlockTypes.states[i];
                if (state != null) {
                    ordinals[i] = predicate.test(state);
                }
            }
        }
        return this;
    }

    public BlockMask add(BlockState... states) {
        addStates(Arrays.asList(states));
        return this;
    }

    public BlockMask remove(BlockState... states) {
        for (BlockState state : states) {
            ordinals[state.getOrdinal()] = false;
        }
        return this;
    }

    public BlockMask clear() {
        Arrays.fill(ordinals, false);
        return this;
    }

    public boolean isEmpty() {
        for (boolean value : ordinals) {
            if (value) {
                return false;
            }
        }
        return true;
    }

    private BlockMask addStates(Collection<BlockState> states) {
        for (BlockState state : states) {
            ordinals[state.getOrdinal()] = true;
        }
        return this;
    }

    public BlockMask add(BlockType... types) {
        addTypes(Arrays.asList(types));
        return this;
    }

    private BlockMask addTypes(Collection<BlockType> types) {
        for (BlockType type : types) {
            for (BlockState state : type.getAllStates()) {
                ordinals[state.getOrdinal()] = true;
            }
        }
        return this;
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param blocks a list of blocks
     * @deprecated NBT not supported by this mask
     */
    @Deprecated
    public void add(Collection<BaseBlock> blocks) {
        for (BaseBlock block : blocks) {
            add(block.toBlockState());
        }
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param block an array of blocks
     */
    public void add(BaseBlock... block) {
        add(Arrays.asList(checkNotNull(block)));
    }

    @Override
    public boolean test(BlockState state) {
        return ordinals[state.getOrdinal()];
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return ordinals[vector.getOrdinal(getExtent())];
    }

    @Override
    public Mask tryCombine(Mask mask) {
        if (mask instanceof ABlockMask) {
            ABlockMask other = (ABlockMask) mask;
            for (int i = 0; i < ordinals.length; i++) {
                if (ordinals[i]) {
                    ordinals[i] = other.test(BlockState.getFromOrdinal(i));
                }
            }
            return this;
        }
        return null;
    }

    @Override
    public Mask tryOr(Mask mask) {
        if (mask instanceof ABlockMask) {
            ABlockMask other = (ABlockMask) mask;
            for (int i = 0; i < ordinals.length; i++) {
                if (!ordinals[i]) {
                    ordinals[i] = other.test(BlockState.getFromOrdinal(i));
                }
            }
            return this;
        }
        return null;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

    @Override
    public Mask tryOptimize() {
        int setStates = 0;
        BlockState setState = null;
        BlockState unsetState = null;
        int totalStates = 0;

        int setTypes = 0;
        BlockType setType = null;
        BlockType unsetType = null;
        int totalTypes = 0;

        for (BlockType type : BlockTypes.values) {
            if (type != null) {
                totalTypes++;
                boolean hasAll = true;
                boolean hasAny = false;
                List<BlockState> all = type.getAllStates();
                for (BlockState state : all) {
                    totalStates++;
                    hasAll &= test(state);
                    hasAny = true;
                }
                if (hasAll) {
                    setTypes++;
                    setType = type;
                    setStates += all.size();
                    setState = type.getDefaultState();
                } else if (hasAny) {
                    for (BlockState state : all) {
                        if (test(state)) {
                            setStates++;
                            setState = state;
                        } else {
                            unsetState = state;
                        }
                    }
                } else {
                    unsetType = type;
                }
            }
        }
        if (setStates == 0) {
            return Masks.alwaysFalse();
        }
        if (setStates == totalStates) {
            return Masks.alwaysTrue();
        }

        if (setStates == 1) {
            return new SingleBlockStateMask(getExtent(), setState);
        }

        if (setStates == totalStates - 1) {
            return new InverseSingleBlockStateMask(getExtent(), unsetState);
        }

        if (setTypes == 1) {
            return new SingleBlockTypeMask(getExtent(), setType);
        }

        if (setTypes == totalTypes - 1) {
            if (unsetType != null) {
                return new InverseSingleBlockTypeMask(getExtent(), unsetType);
            } else {
                throw new IllegalArgumentException("unsetType cannot be null when passed to InverseSingleBlockTypeMask");
            }
        }

        return null;
    }

    @Override
    public Mask inverse() {
        boolean[] cloned = ordinals.clone();
        for (int i = 0; i < cloned.length; i++) {
            cloned[i] = !cloned[i];
        }
        return new BlockMask(getExtent(), cloned);
    }
}
