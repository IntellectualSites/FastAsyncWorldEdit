package com.sk89q.worldedit.registry.state;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class PropertyGroup<G, A> {
    public static final PropertyGroup<Integer, Integer> LEVEL = new PropertyGroupBuilder<Integer, Integer>()
            .add(PropertyKey.LEVEL)
            .add(PropertyKey.LAYERS, (Function<Integer, Integer>) o -> o << 1, (Function<Integer, Integer>) o -> o >> 1)
            .setDefault(15)
            .build();


    private static class PropertyFunction<G, A> {
        private final Function<A, A> setFunc;
        private final Function<G, G> getFunc;
        private final Property key;

        public PropertyFunction(Property key, Function<G, G> getProcessor, Function<A, A> setProcessor) {
            this.key = key;
            this.getFunc = getProcessor;
            this.setFunc = setProcessor;
        }
    }

    public static class PropertyGroupBuilder<G, A> {
        private final List<Object[]> funcs = new ArrayList<>();
        private G defaultValue;

        public PropertyGroupBuilder() {
        }

        public PropertyGroupBuilder add(PropertyKey key) {
            return add(key, null, null);
        }

        public PropertyGroupBuilder add(PropertyKey key, Function<Object, G> getProcessor, Function<A, A> setProcessor) {
            if (getProcessor == null) getProcessor = VOID_FUNCTION;
            if (setProcessor == null) setProcessor = VOID_FUNCTION;
            Object[] pf = new Object[]{key, getProcessor, setProcessor};
            funcs.add(pf);
            return this;
        }

        public PropertyGroupBuilder setDefault(G value) {
            this.defaultValue = value;
            return this;
        }

        public PropertyGroup build() {
            PropertyFunction[] states = new PropertyFunction[BlockTypes.size()];
            Property prop;
            for (BlockType type : BlockTypesCache.values) {
                for (Object[] func : funcs) {
                    if ((prop = type.getProperty((PropertyKey) func[0])) != null) {
                        PropertyFunction pf = new PropertyFunction(prop, (Function) func[1], (Function) func[2]);
                        states[type.getInternalId()] = pf;
                        break;
                    }
                }
            }
            return new PropertyGroup(states, defaultValue);
        }
    }

    private final G defaultValue;
    private final PropertyFunction[] states;
    private PropertyGroup(PropertyFunction[] states, G defaultValue) {
        this.states = states;
        this.defaultValue = defaultValue;
    }

    private static final Function VOID_FUNCTION = o -> o;

    public <B extends BlockStateHolder<B>> G get(BlockStateHolder<B> state) {
        BlockType type = state.getBlockType();
        PropertyFunction func = states[type.getInternalId()];
        if (func == null) return defaultValue;
        Object value = state.getState(func.key);
        return (G) func.getFunc.apply(value);
    }

    public <B extends BlockStateHolder<B>> BlockStateHolder<B> set(BlockStateHolder<B> state, A value) {
        BlockType type = state.getBlockType();
        PropertyFunction func = states[type.getInternalId()];
        if (func != null) {
            value = (A) func.setFunc.apply(value);
            state = state.with(func.key, value);
        }
        return state;
    }
}
