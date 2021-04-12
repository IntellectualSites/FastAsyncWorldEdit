package com.boydti.fawe.beta.implementation.lighting;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;

public enum HeightMapType {
    MOTION_BLOCKING {
        @Override
        public boolean blocks(BlockState state) {
            return state.getMaterial().isSolid() || HeightMapType.isFluid(state);
        }
    },
    MOTION_BLOCKING_NO_LEAVES {
        @Override
        public boolean blocks(BlockState state) {
            return (state.getMaterial().isSolid() || HeightMapType.isFluid(state)) && !HeightMapType.isLeaf(state);
        }
    },
    OCEAN_FLOOR {
        @Override
        public boolean blocks(BlockState state) {
            return state.getMaterial().isSolid();
        }
    },
    WORLD_SURFACE {
        @Override
        public boolean blocks(BlockState state) {
            return !state.isAir();
        }
    };

    private static boolean isLeaf(BlockState state) {
        return BlockCategories.LEAVES.contains(state);
    }

    private static boolean isFluid(BlockState state) {
        if (state.getMaterial().isLiquid()) return true;
        Property<Boolean> waterlogged = state.getBlockType().getProperty(PropertyKey.WATERLOGGED);
        if (waterlogged == null) return false;
        return state.getState(waterlogged);
    }

    public abstract boolean blocks(BlockState state);
}
