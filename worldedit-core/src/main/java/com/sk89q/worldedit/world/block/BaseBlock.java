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

package com.sk89q.worldedit.world.block;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.enginehub.linbus.format.snbt.LinStringIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private final LazyReference<LinCompoundTag> nbtData;

    //FAWE start

    /**
     * Construct a block with the given type and default data.
     *
     * @param blockType The block type
     * @deprecated FAWE deprecation - Just use the {@link BlockType#getDefaultState()}
     */
    @Deprecated
    public BaseBlock(BlockType blockType) {
        this(blockType.getDefaultState());
    }
    //FAWE end

    //FAWE start - made public from protected

    /**
     * Construct a block with a state.
     *
     * @param blockState The blockstate
     */
    public BaseBlock(BlockState blockState) {
        this.blockState = blockState;
        this.nbtData = null;
    }
    //FAWE end

    //FAWE start - deprecated upstream method and replaced CompoundTag with LR

    /**
     * Construct a block with the given ID, data value and NBT data structure.
     *
     * @param state   The block state
     * @param nbtData NBT data, which must be provided
     */
    @Deprecated
    public BaseBlock(BlockState state, CompoundTag nbtData) {
        this(state, LazyReference.from(checkNotNull(nbtData)::toLinTag));
    }
    //FAWE end


    /**
     * Construct a block with the given ID, data value and NBT data structure.
     *
     * @param state   The block state
     * @param nbtData NBT data, which must be provided
     */
    protected BaseBlock(BlockState state, LazyReference<LinCompoundTag> nbtData) {
        checkNotNull(nbtData);
        this.blockState = state;
        this.nbtData = nbtData;
    }

    //FAWE start

    /**
     * Construct a block with the given ID and data value.
     *
     * @param id   ID value
     * @param data data value
     */
    @Deprecated
    public BaseBlock(int id, int data) {
        this(getState(id, data));
    }

    public static BlockState getState(int id, int data) {
        BlockState blockState = LegacyMapper.getInstance().getBlockFromLegacy(id, data);
        if (blockState == null) {
            blockState = BlockTypes.AIR.getDefaultState();
        }
        return blockState;
    }
    //FAWE end

    /**
     * Gets a map of state to state values.
     *
     * @return The state map
     */
    @Override
    public Map<Property<?>, Object> getStates() {
        return toImmutableState().getStates();
    }

    @Override
    public BlockType getBlockType() {
        return this.blockState.getBlockType();
    }

    @Override
    public <V> BaseBlock with(Property<V> property, V value) {
        return toImmutableState().with(property, value).toBaseBlock(getNbtReference());
    }

    /**
     * Gets the State for this Block.
     *
     * @param property The state to get the value for
     * @return The state value
     */
    @Override
    public <V> V getState(Property<V> property) {
        return toImmutableState().getState(property);
    }

    @Override
    public String getNbtId() {
        LazyReference<LinCompoundTag> nbtData = this.nbtData;
        if (nbtData == null) {
            return "";
        }
        return nbtData.getValue().getTag("id", LinTagType.stringTag()).value();
    }

    @Nullable
    @Override
    public LazyReference<LinCompoundTag> getNbtReference() {
        return this.nbtData;
    }

    @Override
    public void setNbtReference(@Nullable LazyReference<LinCompoundTag> nbtData) {
        throw new UnsupportedOperationException("This class is immutable.");
    }

    /**
     * Checks whether the type ID and data value are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseBlock)) {
            if (nbtData == null && o instanceof BlockStateHolder) {
                return Objects.equals(toImmutableState(), ((BlockStateHolder<?>) o).toImmutableState());
            }
            return false;
        }

        final BaseBlock otherBlock = (BaseBlock) o;

        return this.blockState.equalsFuzzy(otherBlock.blockState) && Objects.equals(getNbt(), otherBlock.getNbt());
    }

    //FAWE start
    @Override
    public int getInternalId() {
        return blockState.getInternalId();
    }

    @Override
    public BlockMaterial getMaterial() {
        return blockState.getMaterial();
    }

    @Override
    public int getOrdinal() {
        return blockState.getOrdinal();
    }

    @Override
    public final char getOrdinalChar() {
        return blockState.getOrdinalChar();
    }
    //FAWE end

    /**
     * Checks if the type is the same, and if the matched states are the same.
     *
     * @param o other block
     * @return true if equal
     */
    @Override
    public boolean equalsFuzzy(BlockStateHolder<?> o) {
        return this.blockState.equalsFuzzy(o);
    }

    @Override
    public BlockState toImmutableState() {
        return this.blockState;
    }

    @Override
    public BaseBlock toBaseBlock() {
        return this;
    }

    @Override
    public BaseBlock toBaseBlock(LazyReference<LinCompoundTag> compoundTag) {
        if (compoundTag == null) {
            return this.blockState.toBaseBlock();
        } else if (compoundTag == this.nbtData) {
            return this;
        } else {
            return new BaseBlock(this.blockState, compoundTag);
        }
    }

    //FAWE start
    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        set.setFullBlock(extent, this);
        return true;
    }

    @Override
    public void applyTileEntity(OutputExtent output, int x, int y, int z) {
        CompoundTag nbt = getNbtData();
        if (nbt != null) {
            output.setTile(x, y, z, nbt);
        }
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
    public <V> BaseBlock with(PropertyKey property, V value) {
        return toImmutableState().with(property, value).toBaseBlock(getNbtData());
    }

    @Override
    public <V> V getState(PropertyKey property) {
        return toImmutableState().getState(property);
    }

    public BlockState toBlockState() {
        return blockState;
    }

    @Override
    public int hashCode() {
        int ret = getOrdinal() << 3;
        LinCompoundTag nbtData = getNbt();
        if (nbtData != null) {
            ret += nbtData.hashCode();
        }
        return ret;
    }
    //FAWE end

    @Override
    public String toString() {
        String nbtString = "";
        if (nbtData != null) {
            nbtString = LinStringIO.writeToString(nbtData.getValue());
        }

        return blockState.getAsString() + nbtString;
    }

}
