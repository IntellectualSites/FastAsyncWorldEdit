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

import com.fastasyncworldedit.core.queue.ITileInput;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.DeprecationUtil;
import com.sk89q.worldedit.internal.util.NonAbstractForCompatibility;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

//FAWE start - TileEntityBlock
public interface BlockStateHolder<B extends BlockStateHolder<B>> extends TileEntityBlock, Pattern {
//FAWE end

    /**
     * Get the block type.
     *
     * @return The type
     */
    BlockType getBlockType();

    //FAWE start

    /**
     * Magic number (legacy uses).
     */
    @Deprecated
    B withPropertyId(int propertyId);

    /**
     * Get combined id (legacy uses).
     */
    @Deprecated
    int getInternalId();

    @Deprecated
    int getOrdinal();

    @Deprecated
    char getOrdinalChar();

    BlockMaterial getMaterial();

    /**
     * Get type id (legacy uses).
     */
    @Deprecated
    int getInternalBlockTypeId();

    /**
     * Get the block data (legacy uses).
     */
    @Deprecated
    int getInternalPropertiesId();
    //FAWE end

    /**
     * Returns a BlockState with the given state and value applied.
     *
     * @param property The state
     * @param value    The value
     * @return The modified state, or same if could not be applied
     */
    <V> B with(final Property<V> property, final V value);

    /**
     * Gets the value for the given state.
     *
     * @param property The state
     * @return The value
     */
    <V> V getState(Property<V> property);

    //FAWE start

    /**
     * Returns a BlockStateHolder with the given state and value applied.
     *
     * @param property The property key
     * @param value    The value
     * @return The modified state, or same if could not be applied
     */
    <V> B with(final PropertyKey property, final V value);

    /**
     * Gets the value for the given state.
     *
     * @param property The state
     * @return The value
     */
    <V> V getState(final PropertyKey property);
    //FAWE end

    /**
     * Gets an immutable collection of the states.
     *
     * @return The states
     */
    Map<Property<?>, Object> getStates();

    /**
     * Checks if the type is the same, and if the matched states are the same.
     *
     * @param o other block
     * @return true if equal
     */
    boolean equalsFuzzy(BlockStateHolder<?> o);

    /**
     * Returns an immutable {@link BlockState} from this BlockStateHolder.
     *
     * @return A BlockState
     */
    BlockState toImmutableState();

    /**
     * Gets a {@link BaseBlock} from this BlockStateHolder.
     *
     * @return The BaseBlock
     */
    BaseBlock toBaseBlock();

    /**
     * Gets a {@link BaseBlock} from this BlockStateHolder.
     *
     * @param compoundTag The NBT Data to apply
     * @return The BaseBlock
     * @deprecated Use {@link BlockStateHolder#toBaseBlock(LazyReference)}.
     */
    @Deprecated
    default BaseBlock toBaseBlock(CompoundTag compoundTag) {
        return toBaseBlock(compoundTag == null ? null : LazyReference.from(compoundTag::toLinTag));
    }

    /**
     * Gets a {@link BaseBlock} from this BlockStateHolder.
     *
     * @param compoundTag The NBT Data to apply
     * @return The BaseBlock
     *         This must be overridden by new subclasses. See {@link NonAbstractForCompatibility}
     *         for details
     */
    @SuppressWarnings("deprecation")
    @NonAbstractForCompatibility(
            delegateName = "toBaseBlock",
            delegateParams = {CompoundTag.class}
    )
    default BaseBlock toBaseBlock(LazyReference<LinCompoundTag> compoundTag) {
        DeprecationUtil.checkDelegatingOverride(getClass());

        return toBaseBlock(compoundTag == null ? null : new CompoundTag(compoundTag.getValue()));
    }

    /**
     * Gets a {@link BaseBlock} from this BlockStateHolder.
     *
     * @param compoundTag The NBT Data to apply
     * @return The BaseBlock
     */
    default BaseBlock toBaseBlock(LinCompoundTag compoundTag) {
        return toBaseBlock(compoundTag == null ? null : LazyReference.computed(compoundTag));
    }

    @Override
    default BaseBlock applyBlock(BlockVector3 position) {
        return toBaseBlock();
    }

    //FAWE start
    void applyTileEntity(OutputExtent output, int x, int y, int z);

    default BaseBlock toBaseBlock(ITileInput input, int x, int y, int z) {
        throw new UnsupportedOperationException("State is immutable");
    }
    //FAWE end

    default String getAsString() {
        if (getStates().isEmpty()) {
            return this.getBlockType().id();
        } else {
            String properties = getStates().entrySet().stream()
                    .map(entry -> entry.getKey().getName()
                            + "="
                            + entry.getValue().toString().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(","));
            return this.getBlockType().id() + "[" + properties + "]";
        }
    }

}
