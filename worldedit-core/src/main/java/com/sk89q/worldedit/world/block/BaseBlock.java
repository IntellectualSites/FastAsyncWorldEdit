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

package com.sk89q.worldedit.world.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a "snapshot" of a block with NBT Data.
 *
 * <p>An instance of this block contains all the information needed to
 * accurately reproduce the block, provided that the instance was
 * made correctly. In some implementations, it may not be possible to get a
 * snapshot of blocks correctly, so, for example, the NBT data for a block
 * may be missing.</p>
 */
public class BaseBlock implements BlockStateHolder<BaseBlock>, TileEntityBlock {
    private final BlockState blockState;

    @Nullable
    protected CompoundTag nbtData;

    @Deprecated
    public BaseBlock() {
        this(BlockTypes.AIR.getDefaultState());
    }

//    /**
//     * Construct a block with a state.
//     * @deprecated Just use the BlockStateHolder instead
//     * @param blockState The blockstate
//     */
//    @Deprecated
//    public BaseBlock(BlockStateHolder blockState) {
//        this(blockState, blockState.getNbtData());
//    }

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
     * Construct a block with a state.
     *
     * @param blockState The blockstate
     */

    public BaseBlock(BlockState blockState) {
//        this(blockState, blockState.getNbtData());
    	this.blockState = blockState;
    }

    /**
     * Construct a block with the given ID, data value and NBT data structure.
     *
     * @param state The block state
     * @param nbtData NBT data, which must be provided
     */
    public BaseBlock(BlockState state, CompoundTag nbtData) {
        checkNotNull(nbtData);
        this.blockState = state;
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
        this(BlockState.getFromInternalId(internalId), nbtData);
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
            if (!hasNbtData() && o instanceof BlockStateHolder) {
                return Objects.equals(toImmutableState(), ((BlockStateHolder<?>) o).toImmutableState());
            }
            return false;
        }

        final BaseBlock otherBlock = (BaseBlock) o;

        return this.blockState.equalsFuzzy(otherBlock.blockState) && Objects.equals(getNbtData(), otherBlock.getNbtData());
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
    public BlockMaterial getMaterial() {
        return blockState.getMaterial();
    }

    @Override
    public BlockType getBlockType() {
    	return blockState.getBlockType();
    }

    @Override
    public int getOrdinal() {
        return blockState.getOrdinal();
    }

    @Override
    public BaseBlock toBaseBlock() {
        return this;
    }

    @Override
    public BaseBlock toBaseBlock(CompoundTag compoundTag) {
        if (compoundTag == null) {
            return this.blockState.toBaseBlock();
        } else if (compoundTag == this.nbtData) {
            return this;
        } else {
            return new BaseBlock(this.blockState, compoundTag);
        }
    }

    @Override
    public int hashCode() {
        return getOrdinal();
    }

    @Override
    public String toString() {
        if (this.getNbtData() != null) {
            return getAsString() + " {" + String.valueOf(getNbtData()) + "}";
        } else {
            return getAsString();
        }
    }

	@Override
	public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
		return extent.setBlock(set, this);
	}

	@Override
	public boolean hasNbtData() {
		return this.nbtData != null;
	}

	@Override
	public BaseBlock withPropertyId(int propertyId) {
		return getBlockType().withPropertyId(propertyId).toBaseBlock(getNbtData());
	}

	@Override
	public int getInternalBlockTypeId() {
		return toImmutableState().getInternalBlockTypeId();
	}

	@Override
	public int getInternalPropertiesId() {
		return toImmutableState().getInternalPropertiesId();
	}

    @Override
	public <V> BaseBlock with(Property<V> property, V value) {
		return toImmutableState().with(property, value).toBaseBlock(getNbtData());
	}

	@Override
	public <V> BaseBlock with(PropertyKey property, V value) {
		return toImmutableState().with(property, value).toBaseBlock(getNbtData());
	}

	@Override
	public <V> V getState(Property<V> property) {
		return toImmutableState().getState(property);
	}

	@Override
	public <V> V getState(PropertyKey property) {
		return toImmutableState().getState(property);
	}

	@Override
	public Map<Property<?>, Object> getStates() {
		return toImmutableState().getStates();
	}

	@Override
	public boolean equalsFuzzy(BlockStateHolder o) {
		return toImmutableState().equalsFuzzy(o);
	}

}
