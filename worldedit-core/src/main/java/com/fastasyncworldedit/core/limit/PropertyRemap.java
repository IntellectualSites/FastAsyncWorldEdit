package com.fastasyncworldedit.core.limit;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

public record PropertyRemap<T>(Property<T> property, T oldValue, T newValue) {

    /**
     * New instance
     *
     * @param property property to remap values for
     * @param oldValue value to remap from
     * @param newValue value to remap to
     */
    public PropertyRemap {
    }

    /**
     * Apply remapping to a state. Will return original state if property is not present.
     *
     * @param state Block to apply remapping to
     * @return new state
     */
    public <B extends BlockStateHolder<B>> B apply(B state) {
        if (!state.getBlockType().hasProperty(property.getKey())) {
            return state;
        }
        T current = state.getState(property);
        if (current == oldValue) {
            state = state.with(property.getKey(), newValue);
        }
        return state;
    }

    /**
     * Apply remapping to a given value if the given block type has the property associated with this remap instance.
     *
     * @param type  block type to check
     * @param value value to remap
     * @return new value
     */
    public T apply(BlockType type, T value) {
        if (type.hasProperty(property.getKey())) {
            return value == oldValue ? newValue : value;
        }
        return value;
    }

}
