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

package com.sk89q.worldedit.blocks;

import com.boydti.fawe.object.string.MutableCharSequence;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.block.*;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Represents a "snapshot" of a block with NBT Data.
 *
 * <p>An instance of this block contains all the information needed to
 * accurately reproduce the block, provided that the instance was
 * made correctly. In some implementations, it may not be possible to get a
 * snapshot of blocks correctly, so, for example, the NBT data for a block
 * may be missing.</p>
 */
public class BaseBlock extends BlockState {
    private BlockState blockState;

    @Nullable
    protected CompoundTag nbtData;

    @Deprecated
    public BaseBlock() {
        this(BlockTypes.AIR.getDefaultState());
    }

    /**
     * Construct a block with a state.
     * @deprecated Just use the BlockStateHolder instead
     * @param blockState The blockstate
     */
    @Deprecated
    public BaseBlock(BlockStateHolder blockState) {
        this(blockState, blockState.getNbtData());
    }

    @Deprecated
    public BaseBlock(BlockTypes id) {
        this(id.getDefaultState());
    }

    /**
     * Construct a block with the given type and default data.
     * @deprecated Just use the BlockType.getDefaultState()
     * @param blockType The block type
     */
    @Deprecated
    public BaseBlock(BlockType blockType) {
        this(blockType.getDefaultState());
    }

    /**
     * Construct a block with the given ID, data value and NBT data structure.
     *
     * @param state The block state
     * @param nbtData NBT data, which may be null
     */
    public BaseBlock(BlockStateHolder state, @Nullable CompoundTag nbtData) {
        super(0);
        this.blockState = state.toImmutableState();
        this.nbtData = nbtData;
    }

    /**
     * Construct a block with the given ID and data value.
     *
     * @param id ID value
     * @param data data value
     */
    @Deprecated
    public BaseBlock(int id, int data) {
        this(getState(id, data));
    }

    private static final BlockState getState(int id, int data) {
        BlockState blockState = LegacyMapper.getInstance().getBlockFromLegacy(id, data);
        if (blockState == null) {
            blockState = BlockTypes.AIR.getDefaultState();
        }
        return blockState;
    }

    protected BaseBlock(int internalId, CompoundTag nbtData) {
        this(BlockState.get(internalId), nbtData);
    }

    @Deprecated
    public static BaseBlock getFromInternalId(int id, CompoundTag nbtData) {
        return new BaseBlock(id, nbtData);
    }

    /**
     * Create a clone of another block.
     *
     * @param other the other block
     */
    @Deprecated
    public BaseBlock(BaseBlock other) {
        this(other.toImmutableState(), other.getNbtData());
    }

    @Override
    public BlockState toFuzzy() {
        return blockState;
    }

//    /**
//     * Get the block's data value.
//     *
//     * Broken - do not use
//     *
//     * @return data value (0-15)
//     */
//    @Deprecated
//    public int getData() {
//        return 0;
//    }

    @Override
    public String getNbtId() {
        CompoundTag nbtData = getNbtData();
        if (nbtData == null) {
            return "";
        }
        Tag idTag = nbtData.getValue().get("id");
        if (idTag instanceof StringTag) {
            return ((StringTag) idTag).getValue();
        } else {
            return "";
        }
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        return this.nbtData;
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        this.nbtData = nbtData;
    }

    /**
     * Checks whether the type ID and data value are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseBlock)) {
            return false;
        }

        final BaseBlock otherBlock = (BaseBlock) o;

        return this.equals(otherBlock) && Objects.equals(getNbtData(), otherBlock.getNbtData());
    }

    @Override
    public final BlockState toImmutableState() {
        return blockState;
    }

    @Override
    public int getInternalId() {
        return blockState.getInternalId();
    }

    @Override
    public int hashCode() {
        return getInternalId();
    }

    @Override
    public String toString() {
        if (this.getNbtData() != null) {
            return getAsString() + " {" + String.valueOf(getNbtData()) + "}";
        } else {
            return getAsString();
        }
    }
}
