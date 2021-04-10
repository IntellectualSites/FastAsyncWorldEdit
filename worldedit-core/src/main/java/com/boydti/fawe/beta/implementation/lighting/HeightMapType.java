package com.boydti.fawe.beta.implementation.lighting;

import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;

public enum HeightMapType {
    MOTION_BLOCKING {
        @Override
        public boolean blocks(BlockState state) {
            return state.getMaterial().isSolid() || state.getMaterial().isLiquid();
        }
    },
    MOTION_BLOCKING_NO_LEAVES {
        @Override
        public boolean blocks(BlockState state) {
            return (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) && !HeightMapType.isLeaf(state);
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

    public abstract boolean blocks(BlockState state);
}
