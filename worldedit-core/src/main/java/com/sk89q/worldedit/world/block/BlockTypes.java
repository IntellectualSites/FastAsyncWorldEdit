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

import com.fastasyncworldedit.core.command.SuggestInputParseException;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores a list of common {@link BlockType BlockTypes}.
 *
 * @see BlockType
 */
@SuppressWarnings("unused")
public final class BlockTypes {

    //FAWE start - init
    // Doesn't really matter what the hardcoded values are, as FAWE will update it on load
    @Nullable
    public static final BlockType __RESERVED__ = init(); // Placeholder for null index (i.e. when block types are represented as primitives)
    @Nullable
    public static final BlockType ACACIA_BUTTON = init();
    @Nullable
    public static final BlockType ACACIA_DOOR = init();
    @Nullable
    public static final BlockType ACACIA_FENCE = init();
    @Nullable
    public static final BlockType ACACIA_FENCE_GATE = init();
    @Nullable
    public static final BlockType ACACIA_HANGING_SIGN = init();
    @Nullable
    public static final BlockType ACACIA_LEAVES = init();
    @Nullable
    public static final BlockType ACACIA_LOG = init();
    @Nullable
    public static final BlockType ACACIA_PLANKS = init();
    @Nullable
    public static final BlockType ACACIA_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType ACACIA_SAPLING = init();
    @Nullable
    public static final BlockType ACACIA_SIGN = init();
    @Nullable
    public static final BlockType ACACIA_SLAB = init();
    @Nullable
    public static final BlockType ACACIA_STAIRS = init();
    @Nullable
    public static final BlockType ACACIA_TRAPDOOR = init();
    @Nullable
    public static final BlockType ACACIA_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType ACACIA_WALL_SIGN = init();
    @Nullable
    public static final BlockType ACACIA_WOOD = init();
    @Nullable
    public static final BlockType ACTIVATOR_RAIL = init();
    @Nullable
    public static final BlockType AIR = init();
    @Nullable
    public static final BlockType ALLIUM = init();
    @Nullable
    public static final BlockType AMETHYST_BLOCK = init();
    @Nullable
    public static final BlockType AMETHYST_CLUSTER = init();
    @Nullable
    public static final BlockType ANCIENT_DEBRIS = init();
    @Nullable
    public static final BlockType ANDESITE = init();
    @Nullable
    public static final BlockType ANDESITE_SLAB = init();
    @Nullable
    public static final BlockType ANDESITE_STAIRS = init();
    @Nullable
    public static final BlockType ANDESITE_WALL = init();
    @Nullable
    public static final BlockType ANVIL = init();
    @Nullable
    public static final BlockType ATTACHED_MELON_STEM = init();
    @Nullable
    public static final BlockType ATTACHED_PUMPKIN_STEM = init();
    @Nullable
    public static final BlockType AZALEA = init();
    @Nullable
    public static final BlockType AZALEA_LEAVES = init();
    @Nullable
    public static final BlockType AZURE_BLUET = init();
    @Nullable
    public static final BlockType BAMBOO = init();
    @Nullable
    public static final BlockType BAMBOO_BLOCK = init();
    @Nullable
    public static final BlockType BAMBOO_BUTTON = init();
    @Nullable
    public static final BlockType BAMBOO_DOOR = init();
    @Nullable
    public static final BlockType BAMBOO_FENCE = init();
    @Nullable
    public static final BlockType BAMBOO_FENCE_GATE = init();
    @Nullable
    public static final BlockType BAMBOO_HANGING_SIGN = init();
    @Nullable
    public static final BlockType BAMBOO_MOSAIC = init();
    @Nullable
    public static final BlockType BAMBOO_MOSAIC_SLAB = init();
    @Nullable
    public static final BlockType BAMBOO_MOSAIC_STAIRS = init();
    @Nullable
    public static final BlockType BAMBOO_PLANKS = init();
    @Nullable
    public static final BlockType BAMBOO_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType BAMBOO_SAPLING = init();
    @Nullable
    public static final BlockType BAMBOO_SIGN = init();
    @Nullable
    public static final BlockType BAMBOO_SLAB = init();
    @Nullable
    public static final BlockType BAMBOO_STAIRS = init();
    @Nullable
    public static final BlockType BAMBOO_TRAPDOOR = init();
    @Nullable
    public static final BlockType BAMBOO_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType BAMBOO_WALL_SIGN = init();
    @Nullable
    public static final BlockType BARREL = init();
    @Nullable
    public static final BlockType BARRIER = init();
    @Nullable
    public static final BlockType BASALT = init();
    @Nullable
    public static final BlockType BEACON = init();
    @Nullable
    public static final BlockType BEDROCK = init();
    @Nullable
    public static final BlockType BEEHIVE = init();
    @Nullable
    public static final BlockType BEETROOTS = init();
    @Nullable
    public static final BlockType BEE_NEST = init();
    @Nullable
    public static final BlockType BELL = init();
    @Nullable
    public static final BlockType BIG_DRIPLEAF = init();
    @Nullable
    public static final BlockType BIG_DRIPLEAF_STEM = init();
    @Nullable
    public static final BlockType BIRCH_BUTTON = init();
    @Nullable
    public static final BlockType BIRCH_DOOR = init();
    @Nullable
    public static final BlockType BIRCH_FENCE = init();
    @Nullable
    public static final BlockType BIRCH_FENCE_GATE = init();
    @Nullable
    public static final BlockType BIRCH_HANGING_SIGN = init();
    @Nullable
    public static final BlockType BIRCH_LEAVES = init();
    @Nullable
    public static final BlockType BIRCH_LOG = init();
    @Nullable
    public static final BlockType BIRCH_PLANKS = init();
    @Nullable
    public static final BlockType BIRCH_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType BIRCH_SAPLING = init();
    @Nullable
    public static final BlockType BIRCH_SIGN = init();
    @Nullable
    public static final BlockType BIRCH_SLAB = init();
    @Nullable
    public static final BlockType BIRCH_STAIRS = init();
    @Nullable
    public static final BlockType BIRCH_TRAPDOOR = init();
    @Nullable
    public static final BlockType BIRCH_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType BIRCH_WALL_SIGN = init();
    @Nullable
    public static final BlockType BIRCH_WOOD = init();
    @Nullable
    public static final BlockType BLACKSTONE = init();
    @Nullable
    public static final BlockType BLACKSTONE_SLAB = init();
    @Nullable
    public static final BlockType BLACKSTONE_STAIRS = init();
    @Nullable
    public static final BlockType BLACKSTONE_WALL = init();
    @Nullable
    public static final BlockType BLACK_BANNER = init();
    @Nullable
    public static final BlockType BLACK_BED = init();
    @Nullable
    public static final BlockType BLACK_CANDLE = init();
    @Nullable
    public static final BlockType BLACK_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType BLACK_CARPET = init();
    @Nullable
    public static final BlockType BLACK_CONCRETE = init();
    @Nullable
    public static final BlockType BLACK_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType BLACK_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType BLACK_SHULKER_BOX = init();
    @Nullable
    public static final BlockType BLACK_STAINED_GLASS = init();
    @Nullable
    public static final BlockType BLACK_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType BLACK_TERRACOTTA = init();
    @Nullable
    public static final BlockType BLACK_WALL_BANNER = init();
    @Nullable
    public static final BlockType BLACK_WOOL = init();
    @Nullable
    public static final BlockType BLAST_FURNACE = init();
    @Nullable
    public static final BlockType BLUE_BANNER = init();
    @Nullable
    public static final BlockType BLUE_BED = init();
    @Nullable
    public static final BlockType BLUE_CANDLE = init();
    @Nullable
    public static final BlockType BLUE_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType BLUE_CARPET = init();
    @Nullable
    public static final BlockType BLUE_CONCRETE = init();
    @Nullable
    public static final BlockType BLUE_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType BLUE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType BLUE_ICE = init();
    @Nullable
    public static final BlockType BLUE_ORCHID = init();
    @Nullable
    public static final BlockType BLUE_SHULKER_BOX = init();
    @Nullable
    public static final BlockType BLUE_STAINED_GLASS = init();
    @Nullable
    public static final BlockType BLUE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType BLUE_TERRACOTTA = init();
    @Nullable
    public static final BlockType BLUE_WALL_BANNER = init();
    @Nullable
    public static final BlockType BLUE_WOOL = init();
    @Nullable
    public static final BlockType BONE_BLOCK = init();
    @Nullable
    public static final BlockType BOOKSHELF = init();
    @Nullable
    public static final BlockType BRAIN_CORAL = init();
    @Nullable
    public static final BlockType BRAIN_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType BRAIN_CORAL_FAN = init();
    @Nullable
    public static final BlockType BRAIN_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType BREWING_STAND = init();
    @Nullable
    public static final BlockType BRICKS = init();
    @Nullable
    public static final BlockType BRICK_SLAB = init();
    @Nullable
    public static final BlockType BRICK_STAIRS = init();
    @Nullable
    public static final BlockType BRICK_WALL = init();
    @Nullable
    public static final BlockType BROWN_BANNER = init();
    @Nullable
    public static final BlockType BROWN_BED = init();
    @Nullable
    public static final BlockType BROWN_CANDLE = init();
    @Nullable
    public static final BlockType BROWN_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType BROWN_CARPET = init();
    @Nullable
    public static final BlockType BROWN_CONCRETE = init();
    @Nullable
    public static final BlockType BROWN_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType BROWN_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType BROWN_MUSHROOM = init();
    @Nullable
    public static final BlockType BROWN_MUSHROOM_BLOCK = init();
    @Nullable
    public static final BlockType BROWN_SHULKER_BOX = init();
    @Nullable
    public static final BlockType BROWN_STAINED_GLASS = init();
    @Nullable
    public static final BlockType BROWN_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType BROWN_TERRACOTTA = init();
    @Nullable
    public static final BlockType BROWN_WALL_BANNER = init();
    @Nullable
    public static final BlockType BROWN_WOOL = init();
    @Nullable
    public static final BlockType BUBBLE_COLUMN = init();
    @Nullable
    public static final BlockType BUBBLE_CORAL = init();
    @Nullable
    public static final BlockType BUBBLE_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType BUBBLE_CORAL_FAN = init();
    @Nullable
    public static final BlockType BUBBLE_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType BUDDING_AMETHYST = init();
    @Nullable
    public static final BlockType CACTUS = init();
    @Nullable
    public static final BlockType CAKE = init();
    @Nullable
    public static final BlockType CALCITE = init();
    @Nullable
    public static final BlockType CALIBRATED_SCULK_SENSOR = init();
    @Nullable
    public static final BlockType CAMPFIRE = init();
    @Nullable
    public static final BlockType CANDLE = init();
    @Nullable
    public static final BlockType CANDLE_CAKE = init();
    @Nullable
    public static final BlockType CARROTS = init();
    @Nullable
    public static final BlockType CARTOGRAPHY_TABLE = init();
    @Nullable
    public static final BlockType CARVED_PUMPKIN = init();
    @Nullable
    public static final BlockType CAULDRON = init();
    @Nullable
    public static final BlockType CAVE_AIR = init();
    @Nullable
    public static final BlockType CAVE_VINES = init();
    @Nullable
    public static final BlockType CAVE_VINES_PLANT = init();
    @Nullable
    public static final BlockType CHAIN = init();
    @Nullable
    public static final BlockType CHAIN_COMMAND_BLOCK = init();
    @Nullable
    public static final BlockType CHERRY_BUTTON = init();
    @Nullable
    public static final BlockType CHERRY_DOOR = init();
    @Nullable
    public static final BlockType CHERRY_FENCE = init();
    @Nullable
    public static final BlockType CHERRY_FENCE_GATE = init();
    @Nullable
    public static final BlockType CHERRY_HANGING_SIGN = init();
    @Nullable
    public static final BlockType CHERRY_LEAVES = init();
    @Nullable
    public static final BlockType CHERRY_LOG = init();
    @Nullable
    public static final BlockType CHERRY_PLANKS = init();
    @Nullable
    public static final BlockType CHERRY_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType CHERRY_SAPLING = init();
    @Nullable
    public static final BlockType CHERRY_SIGN = init();
    @Nullable
    public static final BlockType CHERRY_SLAB = init();
    @Nullable
    public static final BlockType CHERRY_STAIRS = init();
    @Nullable
    public static final BlockType CHERRY_TRAPDOOR = init();
    @Nullable
    public static final BlockType CHERRY_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType CHERRY_WALL_SIGN = init();
    @Nullable
    public static final BlockType CHERRY_WOOD = init();
    @Nullable
    public static final BlockType CHEST = init();
    @Nullable
    public static final BlockType CHIPPED_ANVIL = init();
    @Nullable
    public static final BlockType CHISELED_BOOKSHELF = init();
    @Nullable
    public static final BlockType CHISELED_COPPER = init();
    @Nullable
    public static final BlockType CHISELED_DEEPSLATE = init();
    @Nullable
    public static final BlockType CHISELED_NETHER_BRICKS = init();
    @Nullable
    public static final BlockType CHISELED_POLISHED_BLACKSTONE = init();
    @Nullable
    public static final BlockType CHISELED_QUARTZ_BLOCK = init();
    @Nullable
    public static final BlockType CHISELED_RED_SANDSTONE = init();
    @Nullable
    public static final BlockType CHISELED_SANDSTONE = init();
    @Nullable
    public static final BlockType CHISELED_STONE_BRICKS = init();
    @Nullable
    public static final BlockType CHISELED_TUFF = init();
    @Nullable
    public static final BlockType CHISELED_TUFF_BRICKS = init();
    @Nullable
    public static final BlockType CHORUS_FLOWER = init();
    @Nullable
    public static final BlockType CHORUS_PLANT = init();
    @Nullable
    public static final BlockType CLAY = init();
    @Nullable
    public static final BlockType COAL_BLOCK = init();
    @Nullable
    public static final BlockType COAL_ORE = init();
    @Nullable
    public static final BlockType COARSE_DIRT = init();
    @Nullable
    public static final BlockType COBBLED_DEEPSLATE = init();
    @Nullable
    public static final BlockType COBBLED_DEEPSLATE_SLAB = init();
    @Nullable
    public static final BlockType COBBLED_DEEPSLATE_STAIRS = init();
    @Nullable
    public static final BlockType COBBLED_DEEPSLATE_WALL = init();
    @Nullable
    public static final BlockType COBBLESTONE = init();
    @Nullable
    public static final BlockType COBBLESTONE_SLAB = init();
    @Nullable
    public static final BlockType COBBLESTONE_STAIRS = init();
    @Nullable
    public static final BlockType COBBLESTONE_WALL = init();
    @Nullable
    public static final BlockType COBWEB = init();
    @Nullable
    public static final BlockType COCOA = init();
    @Nullable
    public static final BlockType COMMAND_BLOCK = init();
    @Nullable
    public static final BlockType COMPARATOR = init();
    @Nullable
    public static final BlockType COMPOSTER = init();
    @Nullable
    public static final BlockType CONDUIT = init();
    @Nullable
    public static final BlockType COPPER_BLOCK = init();
    @Nullable
    public static final BlockType COPPER_BULB = init();
    @Nullable
    public static final BlockType COPPER_DOOR = init();
    @Nullable
    public static final BlockType COPPER_GRATE = init();
    @Nullable
    public static final BlockType COPPER_ORE = init();
    @Nullable
    public static final BlockType COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType CORNFLOWER = init();
    @Nullable
    public static final BlockType CRACKED_DEEPSLATE_BRICKS = init();
    @Nullable
    public static final BlockType CRACKED_DEEPSLATE_TILES = init();
    @Nullable
    public static final BlockType CRACKED_NETHER_BRICKS = init();
    @Nullable
    public static final BlockType CRACKED_POLISHED_BLACKSTONE_BRICKS = init();
    @Nullable
    public static final BlockType CRACKED_STONE_BRICKS = init();
    @Nullable
    public static final BlockType CRAFTER = init();
    @Nullable
    public static final BlockType CRAFTING_TABLE = init();
    @Nullable
    public static final BlockType CREEPER_HEAD = init();
    @Nullable
    public static final BlockType CREEPER_WALL_HEAD = init();
    @Nullable
    public static final BlockType CRIMSON_BUTTON = init();
    @Nullable
    public static final BlockType CRIMSON_DOOR = init();
    @Nullable
    public static final BlockType CRIMSON_FENCE = init();
    @Nullable
    public static final BlockType CRIMSON_FENCE_GATE = init();
    @Nullable
    public static final BlockType CRIMSON_FUNGUS = init();
    @Nullable
    public static final BlockType CRIMSON_HANGING_SIGN = init();
    @Nullable
    public static final BlockType CRIMSON_HYPHAE = init();
    @Nullable
    public static final BlockType CRIMSON_NYLIUM = init();
    @Nullable
    public static final BlockType CRIMSON_PLANKS = init();
    @Nullable
    public static final BlockType CRIMSON_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType CRIMSON_ROOTS = init();
    @Nullable
    public static final BlockType CRIMSON_SIGN = init();
    @Nullable
    public static final BlockType CRIMSON_SLAB = init();
    @Nullable
    public static final BlockType CRIMSON_STAIRS = init();
    @Nullable
    public static final BlockType CRIMSON_STEM = init();
    @Nullable
    public static final BlockType CRIMSON_TRAPDOOR = init();
    @Nullable
    public static final BlockType CRIMSON_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType CRIMSON_WALL_SIGN = init();
    @Nullable
    public static final BlockType CRYING_OBSIDIAN = init();
    @Nullable
    public static final BlockType CUT_COPPER = init();
    @Nullable
    public static final BlockType CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType CUT_RED_SANDSTONE = init();
    @Nullable
    public static final BlockType CUT_RED_SANDSTONE_SLAB = init();
    @Nullable
    public static final BlockType CUT_SANDSTONE = init();
    @Nullable
    public static final BlockType CUT_SANDSTONE_SLAB = init();
    @Nullable
    public static final BlockType CYAN_BANNER = init();
    @Nullable
    public static final BlockType CYAN_BED = init();
    @Nullable
    public static final BlockType CYAN_CANDLE = init();
    @Nullable
    public static final BlockType CYAN_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType CYAN_CARPET = init();
    @Nullable
    public static final BlockType CYAN_CONCRETE = init();
    @Nullable
    public static final BlockType CYAN_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType CYAN_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType CYAN_SHULKER_BOX = init();
    @Nullable
    public static final BlockType CYAN_STAINED_GLASS = init();
    @Nullable
    public static final BlockType CYAN_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType CYAN_TERRACOTTA = init();
    @Nullable
    public static final BlockType CYAN_WALL_BANNER = init();
    @Nullable
    public static final BlockType CYAN_WOOL = init();
    @Nullable
    public static final BlockType DAMAGED_ANVIL = init();
    @Nullable
    public static final BlockType DANDELION = init();
    @Nullable
    public static final BlockType DARK_OAK_BUTTON = init();
    @Nullable
    public static final BlockType DARK_OAK_DOOR = init();
    @Nullable
    public static final BlockType DARK_OAK_FENCE = init();
    @Nullable
    public static final BlockType DARK_OAK_FENCE_GATE = init();
    @Nullable
    public static final BlockType DARK_OAK_HANGING_SIGN = init();
    @Nullable
    public static final BlockType DARK_OAK_LEAVES = init();
    @Nullable
    public static final BlockType DARK_OAK_LOG = init();
    @Nullable
    public static final BlockType DARK_OAK_PLANKS = init();
    @Nullable
    public static final BlockType DARK_OAK_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType DARK_OAK_SAPLING = init();
    @Nullable
    public static final BlockType DARK_OAK_SIGN = init();
    @Nullable
    public static final BlockType DARK_OAK_SLAB = init();
    @Nullable
    public static final BlockType DARK_OAK_STAIRS = init();
    @Nullable
    public static final BlockType DARK_OAK_TRAPDOOR = init();
    @Nullable
    public static final BlockType DARK_OAK_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType DARK_OAK_WALL_SIGN = init();
    @Nullable
    public static final BlockType DARK_OAK_WOOD = init();
    @Nullable
    public static final BlockType DARK_PRISMARINE = init();
    @Nullable
    public static final BlockType DARK_PRISMARINE_SLAB = init();
    @Nullable
    public static final BlockType DARK_PRISMARINE_STAIRS = init();
    @Nullable
    public static final BlockType DAYLIGHT_DETECTOR = init();
    @Nullable
    public static final BlockType DEAD_BRAIN_CORAL = init();
    @Nullable
    public static final BlockType DEAD_BRAIN_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType DEAD_BRAIN_CORAL_FAN = init();
    @Nullable
    public static final BlockType DEAD_BRAIN_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType DEAD_BUBBLE_CORAL = init();
    @Nullable
    public static final BlockType DEAD_BUBBLE_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType DEAD_BUBBLE_CORAL_FAN = init();
    @Nullable
    public static final BlockType DEAD_BUBBLE_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType DEAD_BUSH = init();
    @Nullable
    public static final BlockType DEAD_FIRE_CORAL = init();
    @Nullable
    public static final BlockType DEAD_FIRE_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType DEAD_FIRE_CORAL_FAN = init();
    @Nullable
    public static final BlockType DEAD_FIRE_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType DEAD_HORN_CORAL = init();
    @Nullable
    public static final BlockType DEAD_HORN_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType DEAD_HORN_CORAL_FAN = init();
    @Nullable
    public static final BlockType DEAD_HORN_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType DEAD_TUBE_CORAL = init();
    @Nullable
    public static final BlockType DEAD_TUBE_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType DEAD_TUBE_CORAL_FAN = init();
    @Nullable
    public static final BlockType DEAD_TUBE_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType DECORATED_POT = init();
    @Nullable
    public static final BlockType DEEPSLATE = init();
    @Nullable
    public static final BlockType DEEPSLATE_BRICKS = init();
    @Nullable
    public static final BlockType DEEPSLATE_BRICK_SLAB = init();
    @Nullable
    public static final BlockType DEEPSLATE_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType DEEPSLATE_BRICK_WALL = init();
    @Nullable
    public static final BlockType DEEPSLATE_COAL_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_COPPER_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_DIAMOND_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_EMERALD_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_GOLD_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_IRON_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_LAPIS_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_REDSTONE_ORE = init();
    @Nullable
    public static final BlockType DEEPSLATE_TILES = init();
    @Nullable
    public static final BlockType DEEPSLATE_TILE_SLAB = init();
    @Nullable
    public static final BlockType DEEPSLATE_TILE_STAIRS = init();
    @Nullable
    public static final BlockType DEEPSLATE_TILE_WALL = init();
    @Nullable
    public static final BlockType DETECTOR_RAIL = init();
    @Nullable
    public static final BlockType DIAMOND_BLOCK = init();
    @Nullable
    public static final BlockType DIAMOND_ORE = init();
    @Nullable
    public static final BlockType DIORITE = init();
    @Nullable
    public static final BlockType DIORITE_SLAB = init();
    @Nullable
    public static final BlockType DIORITE_STAIRS = init();
    @Nullable
    public static final BlockType DIORITE_WALL = init();
    @Nullable
    public static final BlockType DIRT = init();
    @Nullable
    public static final BlockType DIRT_PATH = init();
    @Nullable
    public static final BlockType DISPENSER = init();
    @Nullable
    public static final BlockType DRAGON_EGG = init();
    @Nullable
    public static final BlockType DRAGON_HEAD = init();
    @Nullable
    public static final BlockType DRAGON_WALL_HEAD = init();
    @Nullable
    public static final BlockType DRIED_KELP_BLOCK = init();
    @Nullable
    public static final BlockType DRIPSTONE_BLOCK = init();
    @Nullable
    public static final BlockType DROPPER = init();
    @Nullable
    public static final BlockType EMERALD_BLOCK = init();
    @Nullable
    public static final BlockType EMERALD_ORE = init();
    @Nullable
    public static final BlockType ENCHANTING_TABLE = init();
    @Nullable
    public static final BlockType ENDER_CHEST = init();
    @Nullable
    public static final BlockType END_GATEWAY = init();
    @Nullable
    public static final BlockType END_PORTAL = init();
    @Nullable
    public static final BlockType END_PORTAL_FRAME = init();
    @Nullable
    public static final BlockType END_ROD = init();
    @Nullable
    public static final BlockType END_STONE = init();
    @Nullable
    public static final BlockType END_STONE_BRICKS = init();
    @Nullable
    public static final BlockType END_STONE_BRICK_SLAB = init();
    @Nullable
    public static final BlockType END_STONE_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType END_STONE_BRICK_WALL = init();
    @Nullable
    public static final BlockType EXPOSED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType EXPOSED_COPPER = init();
    @Nullable
    public static final BlockType EXPOSED_COPPER_BULB = init();
    @Nullable
    public static final BlockType EXPOSED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType EXPOSED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType EXPOSED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType EXPOSED_CUT_COPPER = init();
    @Nullable
    public static final BlockType EXPOSED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType EXPOSED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType FARMLAND = init();
    @Nullable
    public static final BlockType FERN = init();
    @Nullable
    public static final BlockType FIRE = init();
    @Nullable
    public static final BlockType FIRE_CORAL = init();
    @Nullable
    public static final BlockType FIRE_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType FIRE_CORAL_FAN = init();
    @Nullable
    public static final BlockType FIRE_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType FLETCHING_TABLE = init();
    @Nullable
    public static final BlockType FLOWERING_AZALEA = init();
    @Nullable
    public static final BlockType FLOWERING_AZALEA_LEAVES = init();
    @Nullable
    public static final BlockType FROGSPAWN = init();
    @Nullable
    public static final BlockType FLOWER_POT = init();
    @Nullable
    public static final BlockType FROSTED_ICE = init();
    @Nullable
    public static final BlockType FURNACE = init();
    @Nullable
    public static final BlockType GILDED_BLACKSTONE = init();
    @Nullable
    public static final BlockType GLASS = init();
    @Nullable
    public static final BlockType GLASS_PANE = init();
    @Nullable
    public static final BlockType GLOWSTONE = init();
    @Nullable
    public static final BlockType GLOW_LICHEN = init();
    @Nullable
    public static final BlockType GOLD_BLOCK = init();
    @Nullable
    public static final BlockType GOLD_ORE = init();
    @Nullable
    public static final BlockType GRANITE = init();
    @Nullable
    public static final BlockType GRANITE_SLAB = init();
    @Nullable
    public static final BlockType GRANITE_STAIRS = init();
    @Nullable
    public static final BlockType GRANITE_WALL = init();
    @Nullable @Deprecated
    public static final BlockType GRASS = init();
    @Nullable
    public static final BlockType GRASS_BLOCK = init();
    @Deprecated
    @Nullable
    public static final BlockType GRASS_PATH = init();
    @Nullable
    public static final BlockType GRAVEL = init();
    @Nullable
    public static final BlockType GRAY_BANNER = init();
    @Nullable
    public static final BlockType GRAY_BED = init();
    @Nullable
    public static final BlockType GRAY_CANDLE = init();
    @Nullable
    public static final BlockType GRAY_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType GRAY_CARPET = init();
    @Nullable
    public static final BlockType GRAY_CONCRETE = init();
    @Nullable
    public static final BlockType GRAY_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType GRAY_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType GRAY_SHULKER_BOX = init();
    @Nullable
    public static final BlockType GRAY_STAINED_GLASS = init();
    @Nullable
    public static final BlockType GRAY_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType GRAY_TERRACOTTA = init();
    @Nullable
    public static final BlockType GRAY_WALL_BANNER = init();
    @Nullable
    public static final BlockType GRAY_WOOL = init();
    @Nullable
    public static final BlockType GREEN_BANNER = init();
    @Nullable
    public static final BlockType GREEN_BED = init();
    @Nullable
    public static final BlockType GREEN_CANDLE = init();
    @Nullable
    public static final BlockType GREEN_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType GREEN_CARPET = init();
    @Nullable
    public static final BlockType GREEN_CONCRETE = init();
    @Nullable
    public static final BlockType GREEN_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType GREEN_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType GREEN_SHULKER_BOX = init();
    @Nullable
    public static final BlockType GREEN_STAINED_GLASS = init();
    @Nullable
    public static final BlockType GREEN_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType GREEN_TERRACOTTA = init();
    @Nullable
    public static final BlockType GREEN_WALL_BANNER = init();
    @Nullable
    public static final BlockType GREEN_WOOL = init();
    @Nullable
    public static final BlockType GRINDSTONE = init();
    @Nullable
    public static final BlockType HANGING_ROOTS = init();
    @Nullable
    public static final BlockType HAY_BLOCK = init();
    @Nullable
    public static final BlockType HEAVY_CORE = init();
    @Nullable
    public static final BlockType HEAVY_WEIGHTED_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType HONEYCOMB_BLOCK = init();
    @Nullable
    public static final BlockType HONEY_BLOCK = init();
    @Nullable
    public static final BlockType HOPPER = init();
    @Nullable
    public static final BlockType HORN_CORAL = init();
    @Nullable
    public static final BlockType HORN_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType HORN_CORAL_FAN = init();
    @Nullable
    public static final BlockType HORN_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType ICE = init();
    @Nullable
    public static final BlockType INFESTED_CHISELED_STONE_BRICKS = init();
    @Nullable
    public static final BlockType INFESTED_COBBLESTONE = init();
    @Nullable
    public static final BlockType INFESTED_CRACKED_STONE_BRICKS = init();
    @Nullable
    public static final BlockType INFESTED_DEEPSLATE = init();
    @Nullable
    public static final BlockType INFESTED_MOSSY_STONE_BRICKS = init();
    @Nullable
    public static final BlockType INFESTED_STONE = init();
    @Nullable
    public static final BlockType INFESTED_STONE_BRICKS = init();
    @Nullable
    public static final BlockType IRON_BARS = init();
    @Nullable
    public static final BlockType IRON_BLOCK = init();
    @Nullable
    public static final BlockType IRON_DOOR = init();
    @Nullable
    public static final BlockType IRON_ORE = init();
    @Nullable
    public static final BlockType IRON_TRAPDOOR = init();
    @Nullable
    public static final BlockType JACK_O_LANTERN = init();
    @Nullable
    public static final BlockType JIGSAW = init();
    @Nullable
    public static final BlockType JUKEBOX = init();
    @Nullable
    public static final BlockType JUNGLE_BUTTON = init();
    @Nullable
    public static final BlockType JUNGLE_DOOR = init();
    @Nullable
    public static final BlockType JUNGLE_FENCE = init();
    @Nullable
    public static final BlockType JUNGLE_FENCE_GATE = init();
    @Nullable
    public static final BlockType JUNGLE_HANGING_SIGN = init();
    @Nullable
    public static final BlockType JUNGLE_LEAVES = init();
    @Nullable
    public static final BlockType JUNGLE_LOG = init();
    @Nullable
    public static final BlockType JUNGLE_PLANKS = init();
    @Nullable
    public static final BlockType JUNGLE_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType JUNGLE_SAPLING = init();
    @Nullable
    public static final BlockType JUNGLE_SIGN = init();
    @Nullable
    public static final BlockType JUNGLE_SLAB = init();
    @Nullable
    public static final BlockType JUNGLE_STAIRS = init();
    @Nullable
    public static final BlockType JUNGLE_TRAPDOOR = init();
    @Nullable
    public static final BlockType JUNGLE_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType JUNGLE_WALL_SIGN = init();
    @Nullable
    public static final BlockType JUNGLE_WOOD = init();
    @Nullable
    public static final BlockType KELP = init();
    @Nullable
    public static final BlockType KELP_PLANT = init();
    @Nullable
    public static final BlockType LADDER = init();
    @Nullable
    public static final BlockType LANTERN = init();
    @Nullable
    public static final BlockType LAPIS_BLOCK = init();
    @Nullable
    public static final BlockType LAPIS_ORE = init();
    @Nullable
    public static final BlockType LARGE_AMETHYST_BUD = init();
    @Nullable
    public static final BlockType LARGE_FERN = init();
    @Nullable
    public static final BlockType LAVA = init();
    @Nullable
    public static final BlockType LAVA_CAULDRON = init();
    @Nullable
    public static final BlockType LECTERN = init();
    @Nullable
    public static final BlockType LEVER = init();
    @Nullable
    public static final BlockType LIGHT = init();
    @Nullable
    public static final BlockType LIGHTNING_ROD = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_BANNER = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_BED = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_CANDLE = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_CARPET = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_CONCRETE = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_SHULKER_BOX = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_STAINED_GLASS = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_TERRACOTTA = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_WALL_BANNER = init();
    @Nullable
    public static final BlockType LIGHT_BLUE_WOOL = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_BANNER = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_BED = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_CANDLE = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_CARPET = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_CONCRETE = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_SHULKER_BOX = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_STAINED_GLASS = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_TERRACOTTA = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_WALL_BANNER = init();
    @Nullable
    public static final BlockType LIGHT_GRAY_WOOL = init();
    @Nullable
    public static final BlockType LIGHT_WEIGHTED_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType LILAC = init();
    @Nullable
    public static final BlockType LILY_OF_THE_VALLEY = init();
    @Nullable
    public static final BlockType LILY_PAD = init();
    @Nullable
    public static final BlockType LIME_BANNER = init();
    @Nullable
    public static final BlockType LIME_BED = init();
    @Nullable
    public static final BlockType LIME_CANDLE = init();
    @Nullable
    public static final BlockType LIME_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType LIME_CARPET = init();
    @Nullable
    public static final BlockType LIME_CONCRETE = init();
    @Nullable
    public static final BlockType LIME_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType LIME_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType LIME_SHULKER_BOX = init();
    @Nullable
    public static final BlockType LIME_STAINED_GLASS = init();
    @Nullable
    public static final BlockType LIME_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType LIME_TERRACOTTA = init();
    @Nullable
    public static final BlockType LIME_WALL_BANNER = init();
    @Nullable
    public static final BlockType LIME_WOOL = init();
    @Nullable
    public static final BlockType LODESTONE = init();
    @Nullable
    public static final BlockType LOOM = init();
    @Nullable
    public static final BlockType MAGENTA_BANNER = init();
    @Nullable
    public static final BlockType MAGENTA_BED = init();
    @Nullable
    public static final BlockType MAGENTA_CANDLE = init();
    @Nullable
    public static final BlockType MAGENTA_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType MAGENTA_CARPET = init();
    @Nullable
    public static final BlockType MAGENTA_CONCRETE = init();
    @Nullable
    public static final BlockType MAGENTA_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType MAGENTA_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType MAGENTA_SHULKER_BOX = init();
    @Nullable
    public static final BlockType MAGENTA_STAINED_GLASS = init();
    @Nullable
    public static final BlockType MAGENTA_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType MAGENTA_TERRACOTTA = init();
    @Nullable
    public static final BlockType MAGENTA_WALL_BANNER = init();
    @Nullable
    public static final BlockType MAGENTA_WOOL = init();
    @Nullable
    public static final BlockType MAGMA_BLOCK = init();
    @Nullable
    public static final BlockType MANGROVE_BUTTON = init();
    @Nullable
    public static final BlockType MANGROVE_DOOR = init();
    @Nullable
    public static final BlockType MANGROVE_FENCE = init();
    @Nullable
    public static final BlockType MANGROVE_FENCE_GATE = init();
    @Nullable
    public static final BlockType MANGROVE_HANGING_SIGN = init();
    @Nullable
    public static final BlockType MANGROVE_LEAVES = init();
    @Nullable
    public static final BlockType MANGROVE_LOG = init();
    @Nullable
    public static final BlockType MANGROVE_PLANKS = init();
    @Nullable
    public static final BlockType MANGROVE_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType MANGROVE_PROPAGULE = init();
    @Nullable
    public static final BlockType MANGROVE_ROOTS = init();
    @Nullable
    public static final BlockType MANGROVE_SIGN = init();
    @Nullable
    public static final BlockType MANGROVE_SLAB = init();
    @Nullable
    public static final BlockType MANGROVE_STAIRS = init();
    @Nullable
    public static final BlockType MANGROVE_TRAPDOOR = init();
    @Nullable
    public static final BlockType MANGROVE_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType MANGROVE_WALL_SIGN = init();
    @Nullable
    public static final BlockType MANGROVE_WOOD = init();
    @Nullable
    public static final BlockType MEDIUM_AMETHYST_BUD = init();
    @Nullable
    public static final BlockType MELON = init();
    @Nullable
    public static final BlockType MELON_STEM = init();
    @Nullable
    public static final BlockType MOSSY_COBBLESTONE = init();
    @Nullable
    public static final BlockType MOSSY_COBBLESTONE_SLAB = init();
    @Nullable
    public static final BlockType MOSSY_COBBLESTONE_STAIRS = init();
    @Nullable
    public static final BlockType MOSSY_COBBLESTONE_WALL = init();
    @Nullable
    public static final BlockType MOSSY_STONE_BRICKS = init();
    @Nullable
    public static final BlockType MOSSY_STONE_BRICK_SLAB = init();
    @Nullable
    public static final BlockType MOSSY_STONE_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType MOSSY_STONE_BRICK_WALL = init();
    @Nullable
    public static final BlockType MOSS_BLOCK = init();
    @Nullable
    public static final BlockType MOSS_CARPET = init();
    @Nullable
    public static final BlockType MOVING_PISTON = init();
    @Nullable
    public static final BlockType MUD = init();
    @Nullable
    public static final BlockType MUD_BRICK_SLAB = init();
    @Nullable
    public static final BlockType MUD_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType MUD_BRICK_WALL = init();
    @Nullable
    public static final BlockType MUD_BRICKS = init();
    @Nullable
    public static final BlockType MUDDY_MANGROVE_ROOTS = init();
    @Nullable
    public static final BlockType MUSHROOM_STEM = init();
    @Nullable
    public static final BlockType MYCELIUM = init();
    @Nullable
    public static final BlockType NETHERITE_BLOCK = init();
    @Nullable
    public static final BlockType NETHERRACK = init();
    @Nullable
    public static final BlockType NETHER_BRICKS = init();
    @Nullable
    public static final BlockType NETHER_BRICK_FENCE = init();
    @Nullable
    public static final BlockType NETHER_BRICK_SLAB = init();
    @Nullable
    public static final BlockType NETHER_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType NETHER_BRICK_WALL = init();
    @Nullable
    public static final BlockType NETHER_GOLD_ORE = init();
    @Nullable
    public static final BlockType NETHER_PORTAL = init();
    @Nullable
    public static final BlockType NETHER_QUARTZ_ORE = init();
    @Nullable
    public static final BlockType NETHER_SPROUTS = init();
    @Nullable
    public static final BlockType NETHER_WART = init();
    @Nullable
    public static final BlockType NETHER_WART_BLOCK = init();
    @Nullable
    public static final BlockType NOTE_BLOCK = init();
    @Nullable
    public static final BlockType OAK_BUTTON = init();
    @Nullable
    public static final BlockType OAK_DOOR = init();
    @Nullable
    public static final BlockType OAK_FENCE = init();
    @Nullable
    public static final BlockType OAK_FENCE_GATE = init();
    @Nullable
    public static final BlockType OAK_HANGING_SIGN = init();
    @Nullable
    public static final BlockType OAK_LEAVES = init();
    @Nullable
    public static final BlockType OAK_LOG = init();
    @Nullable
    public static final BlockType OAK_PLANKS = init();
    @Nullable
    public static final BlockType OAK_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType OAK_SAPLING = init();
    @Nullable
    public static final BlockType OAK_SIGN = init();
    @Nullable
    public static final BlockType OAK_SLAB = init();
    @Nullable
    public static final BlockType OAK_STAIRS = init();
    @Nullable
    public static final BlockType OAK_TRAPDOOR = init();
    @Nullable
    public static final BlockType OAK_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType OAK_WALL_SIGN = init();
    @Nullable
    public static final BlockType OAK_WOOD = init();
    @Nullable
    public static final BlockType OBSERVER = init();
    @Nullable
    public static final BlockType OBSIDIAN = init();
    @Nullable
    public static final BlockType OCHRE_FROGLIGHT = init();
    @Nullable
    public static final BlockType ORANGE_BANNER = init();
    @Nullable
    public static final BlockType ORANGE_BED = init();
    @Nullable
    public static final BlockType ORANGE_CANDLE = init();
    @Nullable
    public static final BlockType ORANGE_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType ORANGE_CARPET = init();
    @Nullable
    public static final BlockType ORANGE_CONCRETE = init();
    @Nullable
    public static final BlockType ORANGE_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType ORANGE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType ORANGE_SHULKER_BOX = init();
    @Nullable
    public static final BlockType ORANGE_STAINED_GLASS = init();
    @Nullable
    public static final BlockType ORANGE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType ORANGE_TERRACOTTA = init();
    @Nullable
    public static final BlockType ORANGE_TULIP = init();
    @Nullable
    public static final BlockType ORANGE_WALL_BANNER = init();
    @Nullable
    public static final BlockType ORANGE_WOOL = init();
    @Nullable
    public static final BlockType OXEYE_DAISY = init();
    @Nullable
    public static final BlockType OXIDIZED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType OXIDIZED_COPPER = init();
    @Nullable
    public static final BlockType OXIDIZED_COPPER_BULB = init();
    @Nullable
    public static final BlockType OXIDIZED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType OXIDIZED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType OXIDIZED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType OXIDIZED_CUT_COPPER = init();
    @Nullable
    public static final BlockType OXIDIZED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType OXIDIZED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType PACKED_ICE = init();
    @Nullable
    public static final BlockType PACKED_MUD = init();
    @Nullable
    public static final BlockType PEARLESCENT_FROGLIGHT = init();
    @Nullable
    public static final BlockType PEONY = init();
    @Nullable
    public static final BlockType PETRIFIED_OAK_SLAB = init();
    @Nullable
    public static final BlockType PIGLIN_HEAD = init();
    @Nullable
    public static final BlockType PIGLIN_WALL_HEAD = init();
    @Nullable
    public static final BlockType PINK_BANNER = init();
    @Nullable
    public static final BlockType PINK_BED = init();
    @Nullable
    public static final BlockType PINK_CANDLE = init();
    @Nullable
    public static final BlockType PINK_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType PINK_CARPET = init();
    @Nullable
    public static final BlockType PINK_CONCRETE = init();
    @Nullable
    public static final BlockType PINK_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType PINK_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType PINK_PETALS = init();
    @Nullable
    public static final BlockType PINK_SHULKER_BOX = init();
    @Nullable
    public static final BlockType PINK_STAINED_GLASS = init();
    @Nullable
    public static final BlockType PINK_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType PINK_TERRACOTTA = init();
    @Nullable
    public static final BlockType PINK_TULIP = init();
    @Nullable
    public static final BlockType PINK_WALL_BANNER = init();
    @Nullable
    public static final BlockType PINK_WOOL = init();
    @Nullable
    public static final BlockType PISTON = init();
    @Nullable
    public static final BlockType PISTON_HEAD = init();
    @Nullable
    public static final BlockType PITCHER_CROP = init();
    @Nullable
    public static final BlockType PITCHER_PLANT = init();
    @Nullable
    public static final BlockType PLAYER_HEAD = init();
    @Nullable
    public static final BlockType PLAYER_WALL_HEAD = init();
    @Nullable
    public static final BlockType PODZOL = init();
    @Nullable
    public static final BlockType POINTED_DRIPSTONE = init();
    @Nullable
    public static final BlockType POLISHED_ANDESITE = init();
    @Nullable
    public static final BlockType POLISHED_ANDESITE_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_ANDESITE_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_BASALT = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_BRICKS = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_BRICK_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_BRICK_WALL = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_BUTTON = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_BLACKSTONE_WALL = init();
    @Nullable
    public static final BlockType POLISHED_DEEPSLATE = init();
    @Nullable
    public static final BlockType POLISHED_DEEPSLATE_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_DEEPSLATE_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_DEEPSLATE_WALL = init();
    @Nullable
    public static final BlockType POLISHED_DIORITE = init();
    @Nullable
    public static final BlockType POLISHED_DIORITE_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_DIORITE_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_GRANITE = init();
    @Nullable
    public static final BlockType POLISHED_GRANITE_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_GRANITE_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_TUFF = init();
    @Nullable
    public static final BlockType POLISHED_TUFF_SLAB = init();
    @Nullable
    public static final BlockType POLISHED_TUFF_STAIRS = init();
    @Nullable
    public static final BlockType POLISHED_TUFF_WALL = init();
    @Nullable
    public static final BlockType POPPY = init();
    @Nullable
    public static final BlockType POTATOES = init();
    @Nullable
    public static final BlockType POTTED_ACACIA_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_ALLIUM = init();
    @Deprecated //No longer has "bush"
    @Nullable
    public static final BlockType POTTED_AZALEA_BUSH = init();
    @Nullable
    public static final BlockType POTTED_AZALEA = init();
    @Nullable
    public static final BlockType POTTED_AZURE_BLUET = init();
    @Nullable
    public static final BlockType POTTED_BAMBOO = init();
    @Nullable
    public static final BlockType POTTED_BIRCH_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_BLUE_ORCHID = init();
    @Nullable
    public static final BlockType POTTED_BROWN_MUSHROOM = init();
    @Nullable
    public static final BlockType POTTED_CACTUS = init();
    @Nullable
    public static final BlockType POTTED_CHERRY_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_CORNFLOWER = init();
    @Nullable
    public static final BlockType POTTED_CRIMSON_FUNGUS = init();
    @Nullable
    public static final BlockType POTTED_CRIMSON_ROOTS = init();
    @Nullable
    public static final BlockType POTTED_DANDELION = init();
    @Nullable
    public static final BlockType POTTED_DARK_OAK_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_DEAD_BUSH = init();
    @Nullable
    public static final BlockType POTTED_FERN = init();
    @Deprecated //No longer has "bush"
    @Nullable
    public static final BlockType POTTED_FLOWERING_AZALEA_BUSH = init();
    @Nullable
    public static final BlockType POTTED_FLOWERING_AZALEA = init();
    @Nullable
    public static final BlockType POTTED_JUNGLE_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_LILY_OF_THE_VALLEY = init();
    @Nullable
    public static final BlockType POTTED_MANGROVE_PROPAGULE = init();
    @Nullable
    public static final BlockType POTTED_OAK_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_ORANGE_TULIP = init();
    @Nullable
    public static final BlockType POTTED_OXEYE_DAISY = init();
    @Nullable
    public static final BlockType POTTED_PINK_TULIP = init();
    @Nullable
    public static final BlockType POTTED_POPPY = init();
    @Nullable
    public static final BlockType POTTED_RED_MUSHROOM = init();
    @Nullable
    public static final BlockType POTTED_RED_TULIP = init();
    @Nullable
    public static final BlockType POTTED_SPRUCE_SAPLING = init();
    @Nullable
    public static final BlockType POTTED_TORCHFLOWER = init();
    @Nullable
    public static final BlockType POTTED_WARPED_FUNGUS = init();
    @Nullable
    public static final BlockType POTTED_WARPED_ROOTS = init();
    @Nullable
    public static final BlockType POTTED_WHITE_TULIP = init();
    @Nullable
    public static final BlockType POTTED_WITHER_ROSE = init();
    @Nullable
    public static final BlockType POWDER_SNOW = init();
    @Nullable
    public static final BlockType POWDER_SNOW_CAULDRON = init();
    @Nullable
    public static final BlockType POWERED_RAIL = init();
    @Nullable
    public static final BlockType PRISMARINE = init();
    @Nullable
    public static final BlockType PRISMARINE_BRICKS = init();
    @Nullable
    public static final BlockType PRISMARINE_BRICK_SLAB = init();
    @Nullable
    public static final BlockType PRISMARINE_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType PRISMARINE_SLAB = init();
    @Nullable
    public static final BlockType PRISMARINE_STAIRS = init();
    @Nullable
    public static final BlockType PRISMARINE_WALL = init();
    @Nullable
    public static final BlockType PUMPKIN = init();
    @Nullable
    public static final BlockType PUMPKIN_STEM = init();
    @Nullable
    public static final BlockType PURPLE_BANNER = init();
    @Nullable
    public static final BlockType PURPLE_BED = init();
    @Nullable
    public static final BlockType PURPLE_CANDLE = init();
    @Nullable
    public static final BlockType PURPLE_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType PURPLE_CARPET = init();
    @Nullable
    public static final BlockType PURPLE_CONCRETE = init();
    @Nullable
    public static final BlockType PURPLE_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType PURPLE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType PURPLE_SHULKER_BOX = init();
    @Nullable
    public static final BlockType PURPLE_STAINED_GLASS = init();
    @Nullable
    public static final BlockType PURPLE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType PURPLE_TERRACOTTA = init();
    @Nullable
    public static final BlockType PURPLE_WALL_BANNER = init();
    @Nullable
    public static final BlockType PURPLE_WOOL = init();
    @Nullable
    public static final BlockType PURPUR_BLOCK = init();
    @Nullable
    public static final BlockType PURPUR_PILLAR = init();
    @Nullable
    public static final BlockType PURPUR_SLAB = init();
    @Nullable
    public static final BlockType PURPUR_STAIRS = init();
    @Nullable
    public static final BlockType QUARTZ_BLOCK = init();
    @Nullable
    public static final BlockType QUARTZ_BRICKS = init();
    @Nullable
    public static final BlockType QUARTZ_PILLAR = init();
    @Nullable
    public static final BlockType QUARTZ_SLAB = init();
    @Nullable
    public static final BlockType QUARTZ_STAIRS = init();
    @Nullable
    public static final BlockType RAIL = init();
    @Nullable
    public static final BlockType RAW_COPPER_BLOCK = init();
    @Nullable
    public static final BlockType RAW_GOLD_BLOCK = init();
    @Nullable
    public static final BlockType RAW_IRON_BLOCK = init();
    @Nullable
    public static final BlockType REDSTONE_BLOCK = init();
    @Nullable
    public static final BlockType REDSTONE_LAMP = init();
    @Nullable
    public static final BlockType REDSTONE_ORE = init();
    @Nullable
    public static final BlockType REDSTONE_TORCH = init();
    @Nullable
    public static final BlockType REDSTONE_WALL_TORCH = init();
    @Nullable
    public static final BlockType REDSTONE_WIRE = init();
    @Nullable
    public static final BlockType REINFORCED_DEEPSLATE = init();
    @Nullable
    public static final BlockType RED_BANNER = init();
    @Nullable
    public static final BlockType RED_BED = init();
    @Nullable
    public static final BlockType RED_CANDLE = init();
    @Nullable
    public static final BlockType RED_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType RED_CARPET = init();
    @Nullable
    public static final BlockType RED_CONCRETE = init();
    @Nullable
    public static final BlockType RED_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType RED_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType RED_MUSHROOM = init();
    @Nullable
    public static final BlockType RED_MUSHROOM_BLOCK = init();
    @Nullable
    public static final BlockType RED_NETHER_BRICKS = init();
    @Nullable
    public static final BlockType RED_NETHER_BRICK_SLAB = init();
    @Nullable
    public static final BlockType RED_NETHER_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType RED_NETHER_BRICK_WALL = init();
    @Nullable
    public static final BlockType RED_SAND = init();
    @Nullable
    public static final BlockType RED_SANDSTONE = init();
    @Nullable
    public static final BlockType RED_SANDSTONE_SLAB = init();
    @Nullable
    public static final BlockType RED_SANDSTONE_STAIRS = init();
    @Nullable
    public static final BlockType RED_SANDSTONE_WALL = init();
    @Nullable
    public static final BlockType RED_SHULKER_BOX = init();
    @Nullable
    public static final BlockType RED_STAINED_GLASS = init();
    @Nullable
    public static final BlockType RED_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType RED_TERRACOTTA = init();
    @Nullable
    public static final BlockType RED_TULIP = init();
    @Nullable
    public static final BlockType RED_WALL_BANNER = init();
    @Nullable
    public static final BlockType RED_WOOL = init();
    @Nullable
    public static final BlockType REPEATER = init();
    @Nullable
    public static final BlockType REPEATING_COMMAND_BLOCK = init();
    @Nullable
    public static final BlockType RESPAWN_ANCHOR = init();
    @Nullable
    public static final BlockType ROOTED_DIRT = init();
    @Nullable
    public static final BlockType ROSE_BUSH = init();
    @Nullable
    public static final BlockType SAND = init();
    @Nullable
    public static final BlockType SANDSTONE = init();
    @Nullable
    public static final BlockType SANDSTONE_SLAB = init();
    @Nullable
    public static final BlockType SANDSTONE_STAIRS = init();
    @Nullable
    public static final BlockType SANDSTONE_WALL = init();
    @Nullable
    public static final BlockType SCAFFOLDING = init();
    @Nullable
    public static final BlockType SCULK = init();
    @Nullable
    public static final BlockType SCULK_CATALYST = init();
    @Nullable
    public static final BlockType SCULK_SENSOR = init();
    @Nullable
    public static final BlockType SCULK_SHRIEKER = init();
    @Nullable
    public static final BlockType SCULK_VEIN = init();
    @Nullable
    public static final BlockType SEAGRASS = init();
    @Nullable
    public static final BlockType SHORT_GRASS = init();
    @Nullable
    public static final BlockType SEA_LANTERN = init();
    @Nullable
    public static final BlockType SEA_PICKLE = init();
    @Nullable
    public static final BlockType SHROOMLIGHT = init();
    @Nullable
    public static final BlockType SHULKER_BOX = init();
    @Deprecated
    @Nullable
    public static final BlockType SIGN = init();
    @Nullable
    public static final BlockType SKELETON_SKULL = init();
    @Nullable
    public static final BlockType SKELETON_WALL_SKULL = init();
    @Nullable
    public static final BlockType SLIME_BLOCK = init();
    @Nullable
    public static final BlockType SMALL_AMETHYST_BUD = init();
    @Nullable
    public static final BlockType SMALL_DRIPLEAF = init();
    @Nullable
    public static final BlockType SMITHING_TABLE = init();
    @Nullable
    public static final BlockType SMOKER = init();
    @Nullable
    public static final BlockType SMOOTH_BASALT = init();
    @Nullable
    public static final BlockType SMOOTH_QUARTZ = init();
    @Nullable
    public static final BlockType SMOOTH_QUARTZ_SLAB = init();
    @Nullable
    public static final BlockType SMOOTH_QUARTZ_STAIRS = init();
    @Nullable
    public static final BlockType SMOOTH_RED_SANDSTONE = init();
    @Nullable
    public static final BlockType SMOOTH_RED_SANDSTONE_SLAB = init();
    @Nullable
    public static final BlockType SMOOTH_RED_SANDSTONE_STAIRS = init();
    @Nullable
    public static final BlockType SMOOTH_SANDSTONE = init();
    @Nullable
    public static final BlockType SMOOTH_SANDSTONE_SLAB = init();
    @Nullable
    public static final BlockType SMOOTH_SANDSTONE_STAIRS = init();
    @Nullable
    public static final BlockType SMOOTH_STONE = init();
    @Nullable
    public static final BlockType SMOOTH_STONE_SLAB = init();
    @Nullable
    public static final BlockType SNIFFER_EGG = init();
    @Nullable
    public static final BlockType SNOW = init();
    @Nullable
    public static final BlockType SNOW_BLOCK = init();
    @Nullable
    public static final BlockType SOUL_CAMPFIRE = init();
    @Nullable
    public static final BlockType SOUL_FIRE = init();
    @Nullable
    public static final BlockType SOUL_LANTERN = init();
    @Nullable
    public static final BlockType SOUL_SAND = init();
    @Nullable
    public static final BlockType SOUL_SOIL = init();
    @Nullable
    public static final BlockType SOUL_TORCH = init();
    @Nullable
    public static final BlockType SOUL_WALL_TORCH = init();
    @Nullable
    public static final BlockType SPAWNER = init();
    @Nullable
    public static final BlockType SPONGE = init();
    @Nullable
    public static final BlockType SPORE_BLOSSOM = init();
    @Nullable
    public static final BlockType SPRUCE_BUTTON = init();
    @Nullable
    public static final BlockType SPRUCE_DOOR = init();
    @Nullable
    public static final BlockType SPRUCE_FENCE = init();
    @Nullable
    public static final BlockType SPRUCE_FENCE_GATE = init();
    @Nullable
    public static final BlockType SPRUCE_HANGING_SIGN = init();
    @Nullable
    public static final BlockType SPRUCE_LEAVES = init();
    @Nullable
    public static final BlockType SPRUCE_LOG = init();
    @Nullable
    public static final BlockType SPRUCE_PLANKS = init();
    @Nullable
    public static final BlockType SPRUCE_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType SPRUCE_SAPLING = init();
    @Nullable
    public static final BlockType SPRUCE_SIGN = init();
    @Nullable
    public static final BlockType SPRUCE_SLAB = init();
    @Nullable
    public static final BlockType SPRUCE_STAIRS = init();
    @Nullable
    public static final BlockType SPRUCE_TRAPDOOR = init();
    @Nullable
    public static final BlockType SPRUCE_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType SPRUCE_WALL_SIGN = init();
    @Nullable
    public static final BlockType SPRUCE_WOOD = init();
    @Nullable
    public static final BlockType STICKY_PISTON = init();
    @Nullable
    public static final BlockType STONE = init();
    @Nullable
    public static final BlockType STONECUTTER = init();
    @Nullable
    public static final BlockType STONE_BRICKS = init();
    @Nullable
    public static final BlockType STONE_BRICK_SLAB = init();
    @Nullable
    public static final BlockType STONE_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType STONE_BRICK_WALL = init();
    @Nullable
    public static final BlockType STONE_BUTTON = init();
    @Nullable
    public static final BlockType STONE_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType STONE_SLAB = init();
    @Nullable
    public static final BlockType STONE_STAIRS = init();
    @Nullable
    public static final BlockType STRIPPED_ACACIA_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_ACACIA_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_BAMBOO_BLOCK = init();
    @Nullable
    public static final BlockType STRIPPED_BIRCH_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_BIRCH_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_CHERRY_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_CHERRY_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_CRIMSON_HYPHAE = init();
    @Nullable
    public static final BlockType STRIPPED_CRIMSON_STEM = init();
    @Nullable
    public static final BlockType STRIPPED_DARK_OAK_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_DARK_OAK_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_JUNGLE_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_JUNGLE_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_MANGROVE_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_MANGROVE_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_OAK_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_OAK_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_SPRUCE_LOG = init();
    @Nullable
    public static final BlockType STRIPPED_SPRUCE_WOOD = init();
    @Nullable
    public static final BlockType STRIPPED_WARPED_HYPHAE = init();
    @Nullable
    public static final BlockType STRIPPED_WARPED_STEM = init();
    @Nullable
    public static final BlockType STRUCTURE_BLOCK = init();
    @Nullable
    public static final BlockType STRUCTURE_VOID = init();
    @Nullable
    public static final BlockType SUGAR_CANE = init();
    @Nullable
    public static final BlockType SUNFLOWER = init();
    @Nullable
    public static final BlockType SUSPICIOUS_GRAVEL = init();
    @Nullable
    public static final BlockType SUSPICIOUS_SAND = init();
    @Nullable
    public static final BlockType SWEET_BERRY_BUSH = init();
    @Nullable
    public static final BlockType TALL_GRASS = init();
    @Nullable
    public static final BlockType TALL_SEAGRASS = init();
    @Nullable
    public static final BlockType TARGET = init();
    @Nullable
    public static final BlockType TERRACOTTA = init();
    @Nullable
    public static final BlockType TINTED_GLASS = init();
    @Nullable
    public static final BlockType TNT = init();
    @Nullable
    public static final BlockType TORCH = init();
    @Nullable
    public static final BlockType TORCHFLOWER = init();
    @Nullable
    public static final BlockType TORCHFLOWER_CROP = init();
    @Nullable
    public static final BlockType TRAPPED_CHEST = init();
    @Nullable
    public static final BlockType TRIAL_SPAWNER = init();
    @Nullable
    public static final BlockType TRIPWIRE = init();
    @Nullable
    public static final BlockType TRIPWIRE_HOOK = init();
    @Nullable
    public static final BlockType TUBE_CORAL = init();
    @Nullable
    public static final BlockType TUBE_CORAL_BLOCK = init();
    @Nullable
    public static final BlockType TUBE_CORAL_FAN = init();
    @Nullable
    public static final BlockType TUBE_CORAL_WALL_FAN = init();
    @Nullable
    public static final BlockType TUFF = init();
    @Nullable
    public static final BlockType TUFF_BRICK_SLAB = init();
    @Nullable
    public static final BlockType TUFF_BRICK_STAIRS = init();
    @Nullable
    public static final BlockType TUFF_BRICK_WALL = init();
    @Nullable
    public static final BlockType TUFF_BRICKS = init();
    @Nullable
    public static final BlockType TUFF_SLAB = init();
    @Nullable
    public static final BlockType TUFF_STAIRS = init();
    @Nullable
    public static final BlockType TUFF_WALL = init();
    @Nullable
    public static final BlockType TURTLE_EGG = init();
    @Nullable
    public static final BlockType TWISTING_VINES = init();
    @Nullable
    public static final BlockType TWISTING_VINES_PLANT = init();
    @Nullable
    public static final BlockType VAULT = init();
    @Nullable
    public static final BlockType VERDANT_FROGLIGHT = init();
    @Nullable
    public static final BlockType VINE = init();
    @Nullable
    public static final BlockType VOID_AIR = init();
    @Deprecated
    @Nullable
    public static final BlockType WALL_SIGN = init();
    @Nullable
    public static final BlockType WALL_TORCH = init();
    @Nullable
    public static final BlockType WARPED_BUTTON = init();
    @Nullable
    public static final BlockType WARPED_DOOR = init();
    @Nullable
    public static final BlockType WARPED_FENCE = init();
    @Nullable
    public static final BlockType WARPED_FENCE_GATE = init();
    @Nullable
    public static final BlockType WARPED_FUNGUS = init();
    @Nullable
    public static final BlockType WARPED_HANGING_SIGN = init();
    @Nullable
    public static final BlockType WARPED_HYPHAE = init();
    @Nullable
    public static final BlockType WARPED_NYLIUM = init();
    @Nullable
    public static final BlockType WARPED_PLANKS = init();
    @Nullable
    public static final BlockType WARPED_PRESSURE_PLATE = init();
    @Nullable
    public static final BlockType WARPED_ROOTS = init();
    @Nullable
    public static final BlockType WARPED_SIGN = init();
    @Nullable
    public static final BlockType WARPED_SLAB = init();
    @Nullable
    public static final BlockType WARPED_STAIRS = init();
    @Nullable
    public static final BlockType WARPED_STEM = init();
    @Nullable
    public static final BlockType WARPED_TRAPDOOR = init();
    @Nullable
    public static final BlockType WARPED_WALL_HANGING_SIGN = init();
    @Nullable
    public static final BlockType WARPED_WALL_SIGN = init();
    @Nullable
    public static final BlockType WARPED_WART_BLOCK = init();
    @Nullable
    public static final BlockType WATER = init();
    @Nullable
    public static final BlockType WATER_CAULDRON = init();
    @Nullable
    public static final BlockType WAXED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_COPPER_BLOCK = init();
    @Nullable
    public static final BlockType WAXED_COPPER_BULB = init();
    @Nullable
    public static final BlockType WAXED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType WAXED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType WAXED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType WAXED_CUT_COPPER = init();
    @Nullable
    public static final BlockType WAXED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType WAXED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_COPPER_BULB = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_CUT_COPPER = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType WAXED_EXPOSED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_COPPER_BULB = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_CUT_COPPER = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType WAXED_OXIDIZED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_COPPER = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_COPPER_BULB = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_CUT_COPPER = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType WAXED_WEATHERED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType WEATHERED_CHISELED_COPPER = init();
    @Nullable
    public static final BlockType WEATHERED_COPPER = init();
    @Nullable
    public static final BlockType WEATHERED_COPPER_BULB = init();
    @Nullable
    public static final BlockType WEATHERED_COPPER_DOOR = init();
    @Nullable
    public static final BlockType WEATHERED_COPPER_GRATE = init();
    @Nullable
    public static final BlockType WEATHERED_COPPER_TRAPDOOR = init();
    @Nullable
    public static final BlockType WEATHERED_CUT_COPPER = init();
    @Nullable
    public static final BlockType WEATHERED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final BlockType WEATHERED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final BlockType WEEPING_VINES = init();
    @Nullable
    public static final BlockType WEEPING_VINES_PLANT = init();
    @Nullable
    public static final BlockType WET_SPONGE = init();
    @Nullable
    public static final BlockType WHEAT = init();
    @Nullable
    public static final BlockType WHITE_BANNER = init();
    @Nullable
    public static final BlockType WHITE_BED = init();
    @Nullable
    public static final BlockType WHITE_CANDLE = init();
    @Nullable
    public static final BlockType WHITE_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType WHITE_CARPET = init();
    @Nullable
    public static final BlockType WHITE_CONCRETE = init();
    @Nullable
    public static final BlockType WHITE_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType WHITE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType WHITE_SHULKER_BOX = init();
    @Nullable
    public static final BlockType WHITE_STAINED_GLASS = init();
    @Nullable
    public static final BlockType WHITE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType WHITE_TERRACOTTA = init();
    @Nullable
    public static final BlockType WHITE_TULIP = init();
    @Nullable
    public static final BlockType WHITE_WALL_BANNER = init();
    @Nullable
    public static final BlockType WHITE_WOOL = init();
    @Nullable
    public static final BlockType WITHER_ROSE = init();
    @Nullable
    public static final BlockType WITHER_SKELETON_SKULL = init();
    @Nullable
    public static final BlockType WITHER_SKELETON_WALL_SKULL = init();
    @Nullable
    public static final BlockType YELLOW_BANNER = init();
    @Nullable
    public static final BlockType YELLOW_BED = init();
    @Nullable
    public static final BlockType YELLOW_CANDLE = init();
    @Nullable
    public static final BlockType YELLOW_CANDLE_CAKE = init();
    @Nullable
    public static final BlockType YELLOW_CARPET = init();
    @Nullable
    public static final BlockType YELLOW_CONCRETE = init();
    @Nullable
    public static final BlockType YELLOW_CONCRETE_POWDER = init();
    @Nullable
    public static final BlockType YELLOW_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final BlockType YELLOW_SHULKER_BOX = init();
    @Nullable
    public static final BlockType YELLOW_STAINED_GLASS = init();
    @Nullable
    public static final BlockType YELLOW_STAINED_GLASS_PANE = init();
    @Nullable
    public static final BlockType YELLOW_TERRACOTTA = init();
    @Nullable
    public static final BlockType YELLOW_WALL_BANNER = init();
    @Nullable
    public static final BlockType YELLOW_WOOL = init();
    @Nullable
    public static final BlockType ZOMBIE_HEAD = init();
    @Nullable
    public static final BlockType ZOMBIE_WALL_HEAD = init();

    private static Field[] fieldsTmp;
    private static int initIndex = 0;

    // Init each field
    // The order is important
    public static BlockType init() {
        if (fieldsTmp == null) {
            fieldsTmp = BlockTypes.class.getDeclaredFields();
            BlockTypesCache.$NAMESPACES.isEmpty(); // initialize cache
        }
        String name = fieldsTmp[initIndex++].getName().toLowerCase(Locale.ROOT);
        return BlockType.REGISTRY.get(name);
    }

    // Clears memory after initialization
    static {
        fieldsTmp = null;
    }

    /*
     -----------------------------------------------------
                    Parsing
     -----------------------------------------------------
     */

    public static BlockType parse(final String type) throws InputParseException {
        return parse(type, new ParserContext());
    }
    public static BlockType parse(final String type, final ParserContext context) throws InputParseException {
        final String inputLower = type.toLowerCase(Locale.ROOT);
        String input = inputLower;

        if (!input.split("\\[", 2)[0].contains(":")) {
            input = "minecraft:" + input;
        }
        BlockType result = BlockType.REGISTRY.get(input);
        if (result != null) {
            return result;
        }
        if (context.isTryingLegacy()) {
            try {
                BlockStateHolder<BlockState> block = LegacyMapper.getInstance().getBlockFromLegacy(input);
                if (block != null) {
                    return block.getBlockType();
                }
            } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            }
        }

        throw new SuggestInputParseException(Caption.of("fawe.error.invalid-block-type", TextComponent.of(input)), () -> Stream.of(
                        BlockTypesCache.values)
                .filter(b -> StringMan.blockStateMatches(inputLower, b.id()))
                .map(BlockType::id)
                .sorted(StringMan.blockStateComparator(inputLower))
                .collect(Collectors.toList())
        );
    }

    public static Set<String> getNameSpaces() {
        return BlockTypesCache.$NAMESPACES;
    }
    //FAWE end

    /**
     * Gets the {@link BlockType} associated with the given id.
     */
    @Nullable
    public static BlockType get(String id) {
        return BlockType.REGISTRY.get(id);
    }

    //FAWE start
    @Nullable
    public static BlockType get(final CharSequence id) {
        return BlockType.REGISTRY.get(id.toString());
    }

    @Deprecated
    public static BlockType get(final int ordinal) {
        return BlockTypesCache.values[ordinal];
    }

    @Deprecated
    public static BlockType getFromStateId(final int internalStateId) {
        return BlockTypesCache.values[internalStateId & BlockTypesCache.BIT_MASK];
    }

    @Deprecated
    public static BlockType getFromStateOrdinal(final int internalStateOrdinal) {
        return BlockTypesCache.states[internalStateOrdinal].getBlockType();
    }

    public static int size() {
        return BlockTypesCache.values.length;
    }
    //FAWE end

}
