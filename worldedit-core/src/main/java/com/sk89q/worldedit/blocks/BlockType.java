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

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.PlayerDirection;
import com.sk89q.worldedit.registry.state.PropertyGroup;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import jdk.nashorn.internal.ir.Block;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Block types.
 *
 * {@deprecated Please use {@link com.sk89q.worldedit.world.block.BlockType }}
 */
@Deprecated
public class BlockType {

    public static double centralTopLimit(com.sk89q.worldedit.world.block.BlockType type) {
        checkNotNull(type);
        return centralTopLimit(type.getDefaultState());
    }

    public static double centralBottomLimit(BlockStateHolder block) {
        checkNotNull(block);
        BlockTypes type = block.getBlockType();
        switch (type) {
            case CREEPER_WALL_HEAD:
            case DRAGON_WALL_HEAD:
            case PLAYER_WALL_HEAD:
            case ZOMBIE_WALL_HEAD: return 0.25;
            case ACACIA_SLAB:
            case BIRCH_SLAB:
            case BRICK_SLAB:
            case COBBLESTONE_SLAB:
            case DARK_OAK_SLAB:
            case DARK_PRISMARINE_SLAB:
            case JUNGLE_SLAB:
            case NETHER_BRICK_SLAB:
            case OAK_SLAB:
            case PETRIFIED_OAK_SLAB:
            case PRISMARINE_BRICK_SLAB:
            case PRISMARINE_SLAB:
            case PURPUR_SLAB:
            case QUARTZ_SLAB:
            case RED_SANDSTONE_SLAB:
            case SANDSTONE_SLAB:
            case SPRUCE_SLAB:
            case STONE_BRICK_SLAB:
            case STONE_SLAB: {
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
            case ACACIA_TRAPDOOR:
            case BIRCH_TRAPDOOR:
            case DARK_OAK_TRAPDOOR:
            case IRON_TRAPDOOR:
            case JUNGLE_TRAPDOOR:
            case OAK_TRAPDOOR:
            case SPRUCE_TRAPDOOR:
                if (block.getState(PropertyKey.OPEN) == Boolean.TRUE) {
                    return 1;
                } else if ("bottom".equals(block.getState(PropertyKey.HALF))) {
                    return 0.8125;
                } else {
                    return 0;
                }
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case OAK_FENCE_GATE:
            case SPRUCE_FENCE_GATE: return block.getState(PropertyKey.OPEN) == Boolean.TRUE ? 1 : 0;
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
        BlockTypes type = block.getBlockType();
        switch (type) {
            case BLACK_BED:
            case BLUE_BED:
            case BROWN_BED:
            case CYAN_BED:
            case GRAY_BED:
            case GREEN_BED:
            case LIGHT_BLUE_BED:
            case LIGHT_GRAY_BED:
            case LIME_BED:
            case MAGENTA_BED:
            case ORANGE_BED:
            case PINK_BED:
            case PURPLE_BED:
            case RED_BED:
            case WHITE_BED:
            case YELLOW_BED: return 0.5625;
            case BREWING_STAND: return 0.875;
            case CAKE: return (block.getState(PropertyKey.BITES) == (Integer) 6) ? 0 : 0.4375;
            case CAULDRON: return 0.3125;
            case COCOA: return 0.750;
            case ENCHANTING_TABLE: return 0.75;
            case END_PORTAL_FRAME: return block.getState(PropertyKey.EYE) == Boolean.TRUE ? 1 : 0.8125;
            case CREEPER_HEAD:
            case DRAGON_HEAD:
            case PISTON_HEAD:
            case PLAYER_HEAD:
            case ZOMBIE_HEAD: return 0.5;
            case CREEPER_WALL_HEAD:
            case DRAGON_WALL_HEAD:
            case PLAYER_WALL_HEAD:
            case ZOMBIE_WALL_HEAD: return 0.75;
            case ACACIA_FENCE:
            case BIRCH_FENCE:
            case DARK_OAK_FENCE:
            case JUNGLE_FENCE:
            case NETHER_BRICK_FENCE:
            case OAK_FENCE:
            case SPRUCE_FENCE: return 1.5;
            case ACACIA_SLAB:
            case BIRCH_SLAB:
            case BRICK_SLAB:
            case COBBLESTONE_SLAB:
            case DARK_OAK_SLAB:
            case DARK_PRISMARINE_SLAB:
            case JUNGLE_SLAB:
            case NETHER_BRICK_SLAB:
            case OAK_SLAB:
            case PETRIFIED_OAK_SLAB:
            case PRISMARINE_BRICK_SLAB:
            case PRISMARINE_SLAB:
            case PURPUR_SLAB:
            case QUARTZ_SLAB:
            case RED_SANDSTONE_SLAB:
            case SANDSTONE_SLAB:
            case SPRUCE_SLAB:
            case STONE_BRICK_SLAB:
            case STONE_SLAB: {
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
            case LILY_PAD: return 0.015625;
            case REPEATER: return 0.125;
            case SOUL_SAND: return 0.875;
            case COBBLESTONE_WALL:
            case MOSSY_COBBLESTONE_WALL: return 1.5;
            case FLOWER_POT: return 0.375;
            case COMPARATOR: return 0.125;
            case DAYLIGHT_DETECTOR: return 0.375;
            case HOPPER: return 0.625;
            case ACACIA_TRAPDOOR:
            case BIRCH_TRAPDOOR:
            case DARK_OAK_TRAPDOOR:
            case IRON_TRAPDOOR:
            case JUNGLE_TRAPDOOR:
            case OAK_TRAPDOOR:
            case SPRUCE_TRAPDOOR:
                if (block.getState(PropertyKey.OPEN) == Boolean.TRUE) {
                    return 0;
                } else if ("top".equals(block.getState(PropertyKey.HALF))) {
                    return 1;
                } else {
                    return 0.1875;
                }
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case OAK_FENCE_GATE:
            case SPRUCE_FENCE_GATE: return block.getState(PropertyKey.OPEN) == Boolean.TRUE ? 0 : 1.5;
            default:
                if (type.hasProperty(PropertyKey.LAYERS)) {
                    return PropertyGroup.LEVEL.get(block) * 0.0625;
                }
                if (!type.getMaterial().isMovementBlocker()) return 0;
                return 1;

        }
    }
}
