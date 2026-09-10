package com.fastasyncworldedit.core.extent.processor.heightmap;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * This enum represents the different types of height maps available in minecraft.
 * <p>
 * Heightmaps are used to describe the highest position for given {@code (x, z)} coordinates.
 * What's considered as highest position depends on the height map type and the blocks at that column.
 * The highest position is a {@code max(y + 1)} such that the block at {@code (x, y, z)} is
 * {@link #includes(BlockState) included} by the height map type.
 */
public enum HeightMapType {
    MOTION_BLOCKING {
        @Override
        public boolean includes(BlockState state) {
            return isMovementBlocker(state) || HeightMapType.hasFluid(state);
        }
    },
    MOTION_BLOCKING_NO_LEAVES {
        @Override
        public boolean includes(BlockState state) {
            return (isMovementBlocker(state) || HeightMapType.hasFluid(state)) && !HeightMapType.isLeaf(state);
        }
    },
    OCEAN_FLOOR {
        @Override
        public boolean includes(BlockState state) {
            return HeightMapType.isMovementBlocker(state);
        }
    },
    WORLD_SURFACE {
        @Override
        public boolean includes(BlockState state) {
            return !state.isAir();
        }
    };

    private static boolean isMovementBlocker(BlockState state) {
        return SolidBlockMask.isSolid(state);
    }

    static {
        BlockCategories.LEAVES.getAll(); // make sure this category is initialized, otherwise isLeaf might fail
    }

    private static boolean isLeaf(BlockState state) {
        return BlockCategories.LEAVES.contains(state);
    }

    /**
     * Returns whether the block state is a fluid or has an attribute that indicates the presence
     * of fluid.
     *
     * @param state the block state to check.
     * @return {@code true} if the block state has any fluid present.
     */
    private static boolean hasFluid(BlockState state) {
        if (state.getMaterial().isLiquid()) {
            return true;
        }
        if (!state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
            return false;
        }
        Property<Boolean> waterlogged = state.getBlockType().getProperty(PropertyKey.WATERLOGGED);
        if (waterlogged == null) {
            return false;
        }
        return state.getState(waterlogged);
    }

    /**
     * Returns whether the given block state is included by this height map.
     *
     * @param state the block state to check.
     * @return {@code true} if the block is included.
     */
    public abstract boolean includes(BlockState state);
}
