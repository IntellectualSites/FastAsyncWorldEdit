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

import com.sk89q.worldedit.registry.state.PropertyGroup;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlockTypeUtil {

    public static double centralTopLimit(com.sk89q.worldedit.world.block.BlockType type) {
        checkNotNull(type);
        return centralTopLimit(type.getDefaultState());
    }

    public static double centralBottomLimit(BlockStateHolder block) {
        checkNotNull(block);
        BlockType type = block.getBlockType();
        switch (type.getInternalId()) {
            case BlockID.CREEPER_WALL_HEAD:
            case BlockID.DRAGON_WALL_HEAD:
            case BlockID.PLAYER_WALL_HEAD:
            case BlockID.ZOMBIE_WALL_HEAD: return 0.25;
            case BlockID.ACACIA_SLAB:
            case BlockID.BIRCH_SLAB:
            case BlockID.BRICK_SLAB:
            case BlockID.COBBLESTONE_SLAB:
            case BlockID.DARK_OAK_SLAB:
            case BlockID.DARK_PRISMARINE_SLAB:
            case BlockID.JUNGLE_SLAB:
            case BlockID.NETHER_BRICK_SLAB:
            case BlockID.OAK_SLAB:
            case BlockID.PETRIFIED_OAK_SLAB:
            case BlockID.PRISMARINE_BRICK_SLAB:
            case BlockID.PRISMARINE_SLAB:
            case BlockID.PURPUR_SLAB:
            case BlockID.QUARTZ_SLAB:
            case BlockID.RED_SANDSTONE_SLAB:
            case BlockID.SANDSTONE_SLAB:
            case BlockID.SPRUCE_SLAB:
            case BlockID.STONE_BRICK_SLAB:
            case BlockID.STONE_SLAB: {
                String state = (String) block.getState(PropertyKey.TYPE);
                if (state == null) return 0;
                switch (state) {
                    case "double":
                    case "bottom":
                        return 0;
                    case "top":
                        return 0.5;
                }
            }
            case BlockID.ACACIA_TRAPDOOR:
            case BlockID.BIRCH_TRAPDOOR:
            case BlockID.DARK_OAK_TRAPDOOR:
            case BlockID.IRON_TRAPDOOR:
            case BlockID.JUNGLE_TRAPDOOR:
            case BlockID.OAK_TRAPDOOR:
            case BlockID.SPRUCE_TRAPDOOR:
                if (block.getState(PropertyKey.OPEN) == Boolean.TRUE) {
                    return 1;
                } else if ("bottom".equals(block.getState(PropertyKey.HALF))) {
                    return 0.8125;
                } else {
                    return 0;
                }
            case BlockID.ACACIA_FENCE_GATE:
            case BlockID.BIRCH_FENCE_GATE:
            case BlockID.DARK_OAK_FENCE_GATE:
            case BlockID.JUNGLE_FENCE_GATE:
            case BlockID.OAK_FENCE_GATE:
            case BlockID.SPRUCE_FENCE_GATE: return block.getState(PropertyKey.OPEN) == Boolean.TRUE ? 1 : 0;
            default:
                if (type.getMaterial().isMovementBlocker()) return 0;
                return 1;
        }
    }

    /**
     * Returns the y offset a player falls to when falling onto the top of a block at xp+0.5/zp+0.5.
     *
     * @param block the block
     * @return the y offset
     */
    public static double centralTopLimit(BlockStateHolder block) {
        checkNotNull(block);
        BlockType type = block.getBlockType();
        switch (type.getInternalId()) {
            case BlockID.BLACK_BED:
            case BlockID.BLUE_BED:
            case BlockID.BROWN_BED:
            case BlockID.CYAN_BED:
            case BlockID.GRAY_BED:
            case BlockID.GREEN_BED:
            case BlockID.LIGHT_BLUE_BED:
            case BlockID.LIGHT_GRAY_BED:
            case BlockID.LIME_BED:
            case BlockID.MAGENTA_BED:
            case BlockID.ORANGE_BED:
            case BlockID.PINK_BED:
            case BlockID.PURPLE_BED:
            case BlockID.RED_BED:
            case BlockID.WHITE_BED:
            case BlockID.YELLOW_BED: return 0.5625;
            case BlockID.BREWING_STAND: return 0.875;
            case BlockID.CAKE: return (block.getState(PropertyKey.BITES) == (Integer) 6) ? 0 : 0.4375;
            case BlockID.CAULDRON: return 0.3125;
            case BlockID.COCOA: return 0.750;
            case BlockID.ENCHANTING_TABLE: return 0.75;
            case BlockID.END_PORTAL_FRAME: return block.getState(PropertyKey.EYE) == Boolean.TRUE ? 1 : 0.8125;
            case BlockID.CREEPER_HEAD:
            case BlockID.DRAGON_HEAD:
            case BlockID.PISTON_HEAD:
            case BlockID.PLAYER_HEAD:
            case BlockID.ZOMBIE_HEAD: return 0.5;
            case BlockID.CREEPER_WALL_HEAD:
            case BlockID.DRAGON_WALL_HEAD:
            case BlockID.PLAYER_WALL_HEAD:
            case BlockID.ZOMBIE_WALL_HEAD: return 0.75;
            case BlockID.ACACIA_FENCE:
            case BlockID.BIRCH_FENCE:
            case BlockID.DARK_OAK_FENCE:
            case BlockID.JUNGLE_FENCE:
            case BlockID.NETHER_BRICK_FENCE:
            case BlockID.OAK_FENCE:
            case BlockID.SPRUCE_FENCE: return 1.5;
            case BlockID.ACACIA_SLAB:
            case BlockID.BIRCH_SLAB:
            case BlockID.BRICK_SLAB:
            case BlockID.COBBLESTONE_SLAB:
            case BlockID.DARK_OAK_SLAB:
            case BlockID.DARK_PRISMARINE_SLAB:
            case BlockID.JUNGLE_SLAB:
            case BlockID.NETHER_BRICK_SLAB:
            case BlockID.OAK_SLAB:
            case BlockID.PETRIFIED_OAK_SLAB:
            case BlockID.PRISMARINE_BRICK_SLAB:
            case BlockID.PRISMARINE_SLAB:
            case BlockID.PURPUR_SLAB:
            case BlockID.QUARTZ_SLAB:
            case BlockID.RED_SANDSTONE_SLAB:
            case BlockID.SANDSTONE_SLAB:
            case BlockID.SPRUCE_SLAB:
            case BlockID.STONE_BRICK_SLAB:
            case BlockID.STONE_SLAB: {
                String state = (String) block.getState(PropertyKey.TYPE);
                if (state == null) return 0.5;
                switch (state) {
                    case "bottom":
                        return 0.5;
                    case "top":
                    case "double":
                        return 1;
                }
            }
            case BlockID.LILY_PAD: return 0.015625;
            case BlockID.REPEATER: return 0.125;
            case BlockID.SOUL_SAND: return 0.875;
            case BlockID.COBBLESTONE_WALL:
            case BlockID.MOSSY_COBBLESTONE_WALL: return 1.5;
            case BlockID.FLOWER_POT: return 0.375;
            case BlockID.COMPARATOR: return 0.125;
            case BlockID.DAYLIGHT_DETECTOR: return 0.375;
            case BlockID.HOPPER: return 0.625;
            case BlockID.ACACIA_TRAPDOOR:
            case BlockID.BIRCH_TRAPDOOR:
            case BlockID.DARK_OAK_TRAPDOOR:
            case BlockID.IRON_TRAPDOOR:
            case BlockID.JUNGLE_TRAPDOOR:
            case BlockID.OAK_TRAPDOOR:
            case BlockID.SPRUCE_TRAPDOOR:
                if (block.getState(PropertyKey.OPEN) == Boolean.TRUE) {
                    return 0;
                } else if ("top".equals(block.getState(PropertyKey.HALF))) {
                    return 1;
                } else {
                    return 0.1875;
                }
            case BlockID.ACACIA_FENCE_GATE:
            case BlockID.BIRCH_FENCE_GATE:
            case BlockID.DARK_OAK_FENCE_GATE:
            case BlockID.JUNGLE_FENCE_GATE:
            case BlockID.OAK_FENCE_GATE:
            case BlockID.SPRUCE_FENCE_GATE: return block.getState(PropertyKey.OPEN) == Boolean.TRUE ? 0 : 1.5;
            default:
                if (type.hasProperty(PropertyKey.LAYERS)) {
                    return PropertyGroup.LEVEL.get(block) * 0.0625;
                }
                if (!type.getMaterial().isMovementBlocker()) return 0;
                return 1;

        }
    }
}