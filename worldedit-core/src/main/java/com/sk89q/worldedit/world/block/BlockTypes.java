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

import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.sk89q.worldedit.world.registry.Registries;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores a list of common Block String IDs.
 */
public final class BlockTypes {
    // Doesn't really matter what the hardcoded values are, as FAWE will update it on load
    @Nullable public static final BlockType __RESERVED__ = BlockType.DUMMY; // Placeholder for null index (i.e. when block types are represented as primitives)
    @Nullable public static final BlockType ACACIA_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_FENCE_GATE = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_LEAVES = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_PLANKS = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_WALL_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType ACACIA_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType ACTIVATOR_RAIL = BlockType.DUMMY;
    @Nullable public static final BlockType AIR = BlockType.DUMMY;
    @Nullable public static final BlockType ALLIUM = BlockType.DUMMY;
    @Nullable public static final BlockType ANDESITE = BlockType.DUMMY;
    @Nullable public static final BlockType ANDESITE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType ANDESITE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType ANDESITE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType ANVIL = BlockType.DUMMY;
    @Nullable public static final BlockType ATTACHED_MELON_STEM = BlockType.DUMMY;
    @Nullable public static final BlockType ATTACHED_PUMPKIN_STEM = BlockType.DUMMY;
    @Nullable public static final BlockType AZURE_BLUET = BlockType.DUMMY;
    @Nullable public static final BlockType BAMBOO = BlockType.DUMMY;
    @Nullable public static final BlockType BAMBOO_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType BARREL = BlockType.DUMMY;
    @Nullable public static final BlockType BARRIER = BlockType.DUMMY;
    @Nullable public static final BlockType BEACON = BlockType.DUMMY;
    @Nullable public static final BlockType BEDROCK = BlockType.DUMMY;
    @Nullable public static final BlockType BEETROOTS = BlockType.DUMMY;
    @Nullable public static final BlockType BELL = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_FENCE_GATE = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_LEAVES = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_PLANKS = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_WALL_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType BIRCH_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_BED = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType BLACK_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType BLAST_FURNACE = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_BED = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_ICE = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_ORCHID = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType BLUE_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType BONE_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType BOOKSHELF = BlockType.DUMMY;
    @Nullable public static final BlockType BRAIN_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType BRAIN_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType BRAIN_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType BRAIN_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType BREWING_STAND = BlockType.DUMMY;
    @Nullable public static final BlockType BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType BRICK_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_BED = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_MUSHROOM = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_MUSHROOM_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType BROWN_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType BUBBLE_COLUMN = BlockType.DUMMY;
    @Nullable public static final BlockType BUBBLE_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType BUBBLE_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType BUBBLE_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType BUBBLE_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType CACTUS = BlockType.DUMMY;
    @Nullable public static final BlockType CAKE = BlockType.DUMMY;
    @Nullable public static final BlockType CAMPFIRE = BlockType.DUMMY;
    @Nullable public static final BlockType CARROTS = BlockType.DUMMY;
    @Nullable public static final BlockType CARTOGRAPHY_TABLE = BlockType.DUMMY;
    @Nullable public static final BlockType CARVED_PUMPKIN = BlockType.DUMMY;
    @Nullable public static final BlockType CAULDRON = BlockType.DUMMY;
    @Nullable public static final BlockType CAVE_AIR = BlockType.DUMMY;
    @Nullable public static final BlockType CHAIN_COMMAND_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType CHEST = BlockType.DUMMY;
    @Nullable public static final BlockType CHIPPED_ANVIL = BlockType.DUMMY;
    @Nullable public static final BlockType CHISELED_QUARTZ_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType CHISELED_RED_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType CHISELED_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType CHISELED_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType CHORUS_FLOWER = BlockType.DUMMY;
    @Nullable public static final BlockType CHORUS_PLANT = BlockType.DUMMY;
    @Nullable public static final BlockType CLAY = BlockType.DUMMY;
    @Nullable public static final BlockType COAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType COAL_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType COARSE_DIRT = BlockType.DUMMY;
    @Nullable public static final BlockType COBBLESTONE = BlockType.DUMMY;
    @Nullable public static final BlockType COBBLESTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType COBBLESTONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType COBBLESTONE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType COBWEB = BlockType.DUMMY;
    @Nullable public static final BlockType COCOA = BlockType.DUMMY;
    @Nullable public static final BlockType COMMAND_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType COMPARATOR = BlockType.DUMMY;
    @Nullable public static final BlockType COMPOSTER = BlockType.DUMMY;
    @Nullable public static final BlockType CONDUIT = BlockType.DUMMY;
    @Nullable public static final BlockType CORNFLOWER = BlockType.DUMMY;
    @Nullable public static final BlockType CRACKED_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType CRAFTING_TABLE = BlockType.DUMMY;
    @Nullable public static final BlockType CREEPER_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType CREEPER_WALL_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType CUT_RED_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType CUT_RED_SANDSTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType CUT_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType CUT_SANDSTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_BED = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType CYAN_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType DAMAGED_ANVIL = BlockType.DUMMY;
    @Nullable public static final BlockType DANDELION = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_FENCE_GATE = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_LEAVES = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_PLANKS = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_WALL_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_OAK_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_PRISMARINE = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_PRISMARINE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType DARK_PRISMARINE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType DAYLIGHT_DETECTOR = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_BUSH = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_FIRE_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_FIRE_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_FIRE_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_FIRE_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_HORN_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_HORN_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_HORN_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_HORN_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_TUBE_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_TUBE_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_TUBE_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DEAD_TUBE_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType DETECTOR_RAIL = BlockType.DUMMY;
    @Nullable public static final BlockType DIAMOND_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DIAMOND_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType DIORITE = BlockType.DUMMY;
    @Nullable public static final BlockType DIORITE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType DIORITE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType DIORITE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType DIRT = BlockType.DUMMY;
    @Nullable public static final BlockType DISPENSER = BlockType.DUMMY;
    @Nullable public static final BlockType DRAGON_EGG = BlockType.DUMMY;
    @Nullable public static final BlockType DRAGON_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType DRAGON_WALL_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType DRIED_KELP_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType DROPPER = BlockType.DUMMY;
    @Nullable public static final BlockType EMERALD_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType EMERALD_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType ENCHANTING_TABLE = BlockType.DUMMY;
    @Nullable public static final BlockType END_GATEWAY = BlockType.DUMMY;
    @Nullable public static final BlockType END_PORTAL = BlockType.DUMMY;
    @Nullable public static final BlockType END_PORTAL_FRAME = BlockType.DUMMY;
    @Nullable public static final BlockType END_ROD = BlockType.DUMMY;
    @Nullable public static final BlockType END_STONE = BlockType.DUMMY;
    @Nullable public static final BlockType END_STONE_BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType END_STONE_BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType END_STONE_BRICK_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType END_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType ENDER_CHEST = BlockType.DUMMY;
    @Nullable public static final BlockType FARMLAND = BlockType.DUMMY;
    @Nullable public static final BlockType FERN = BlockType.DUMMY;
    @Nullable public static final BlockType FIRE = BlockType.DUMMY;
    @Nullable public static final BlockType FIRE_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType FIRE_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType FIRE_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType FIRE_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType FLETCHING_TABLE = BlockType.DUMMY;
    @Nullable public static final BlockType FLOWER_POT = BlockType.DUMMY;
    @Nullable public static final BlockType FROSTED_ICE = BlockType.DUMMY;
    @Nullable public static final BlockType FURNACE = BlockType.DUMMY;
    @Nullable public static final BlockType GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType GLOWSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType GOLD_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType GOLD_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType GRANITE = BlockType.DUMMY;
    @Nullable public static final BlockType GRANITE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType GRANITE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType GRANITE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType GRASS = BlockType.DUMMY;
    @Nullable public static final BlockType GRASS_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType GRASS_PATH = BlockType.DUMMY;
    @Nullable public static final BlockType GRAVEL = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_BED = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType GRAY_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_BED = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType GREEN_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType GRINDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType HAY_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType HEAVY_WEIGHTED_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType HOPPER = BlockType.DUMMY;
    @Nullable public static final BlockType HORN_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType HORN_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType HORN_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType HORN_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType ICE = BlockType.DUMMY;
    @Nullable public static final BlockType INFESTED_CHISELED_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType INFESTED_COBBLESTONE = BlockType.DUMMY;
    @Nullable public static final BlockType INFESTED_CRACKED_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType INFESTED_MOSSY_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType INFESTED_STONE = BlockType.DUMMY;
    @Nullable public static final BlockType INFESTED_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType IRON_BARS = BlockType.DUMMY;
    @Nullable public static final BlockType IRON_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType IRON_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType IRON_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType IRON_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType JACK_O_LANTERN = BlockType.DUMMY;
    @Nullable public static final BlockType JIGSAW = BlockType.DUMMY;
    @Nullable public static final BlockType JUKEBOX = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_FENCE_GATE = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_LEAVES = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_PLANKS = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_WALL_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType JUNGLE_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType KELP = BlockType.DUMMY;
    @Nullable public static final BlockType KELP_PLANT = BlockType.DUMMY;
    @Nullable public static final BlockType LADDER = BlockType.DUMMY;
    @Nullable public static final BlockType LANTERN = BlockType.DUMMY;
    @Nullable public static final BlockType LAPIS_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType LAPIS_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType LARGE_FERN = BlockType.DUMMY;
    @Nullable public static final BlockType LAVA = BlockType.DUMMY;
    @Nullable public static final BlockType LECTERN = BlockType.DUMMY;
    @Nullable public static final BlockType LEVER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_BED = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_BLUE_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_BED = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_GRAY_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType LIGHT_WEIGHTED_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType LILAC = BlockType.DUMMY;
    @Nullable public static final BlockType LILY_OF_THE_VALLEY = BlockType.DUMMY;
    @Nullable public static final BlockType LILY_PAD = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_BED = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType LIME_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType LOOM = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_BED = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType MAGENTA_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType MAGMA_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType MELON = BlockType.DUMMY;
    @Nullable public static final BlockType MELON_STEM = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_COBBLESTONE = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_COBBLESTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_COBBLESTONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_COBBLESTONE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_STONE_BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_STONE_BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_STONE_BRICK_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType MOSSY_STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType MOVING_PISTON = BlockType.DUMMY;
    @Nullable public static final BlockType MUSHROOM_STEM = BlockType.DUMMY;
    @Nullable public static final BlockType MYCELIUM = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_BRICK_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_BRICK_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_PORTAL = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_QUARTZ_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_WART = BlockType.DUMMY;
    @Nullable public static final BlockType NETHER_WART_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType NETHERRACK = BlockType.DUMMY;
    @Nullable public static final BlockType NOTE_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_FENCE_GATE = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_LEAVES = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_PLANKS = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_WALL_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType OAK_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType OBSERVER = BlockType.DUMMY;
    @Nullable public static final BlockType OBSIDIAN = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_BED = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType ORANGE_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType OXEYE_DAISY = BlockType.DUMMY;
    @Nullable public static final BlockType PACKED_ICE = BlockType.DUMMY;
    @Nullable public static final BlockType PEONY = BlockType.DUMMY;
    @Nullable public static final BlockType PETRIFIED_OAK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_BED = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType PINK_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType PISTON = BlockType.DUMMY;
    @Nullable public static final BlockType PISTON_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType PLAYER_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType PLAYER_WALL_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType PODZOL = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_ANDESITE = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_ANDESITE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_ANDESITE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_DIORITE = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_DIORITE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_DIORITE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_GRANITE = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_GRANITE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType POLISHED_GRANITE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType POPPY = BlockType.DUMMY;
    @Nullable public static final BlockType POTATOES = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_ACACIA_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_ALLIUM = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_AZURE_BLUET = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_BAMBOO = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_BIRCH_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_BLUE_ORCHID = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_BROWN_MUSHROOM = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_CACTUS = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_CORNFLOWER = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_DANDELION = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_DARK_OAK_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_DEAD_BUSH = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_FERN = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_JUNGLE_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_LILY_OF_THE_VALLEY = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_OAK_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_ORANGE_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_OXEYE_DAISY = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_PINK_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_POPPY = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_RED_MUSHROOM = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_RED_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_SPRUCE_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_WHITE_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType POTTED_WITHER_ROSE = BlockType.DUMMY;
    @Nullable public static final BlockType POWERED_RAIL = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE_BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE_BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType PRISMARINE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType PUMPKIN = BlockType.DUMMY;
    @Nullable public static final BlockType PUMPKIN_STEM = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_BED = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType PURPLE_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType PURPUR_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType PURPUR_PILLAR = BlockType.DUMMY;
    @Nullable public static final BlockType PURPUR_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType PURPUR_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType QUARTZ_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType QUARTZ_PILLAR = BlockType.DUMMY;
    @Nullable public static final BlockType QUARTZ_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType QUARTZ_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType RAIL = BlockType.DUMMY;
    @Nullable public static final BlockType RED_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType RED_BED = BlockType.DUMMY;
    @Nullable public static final BlockType RED_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType RED_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType RED_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType RED_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType RED_MUSHROOM = BlockType.DUMMY;
    @Nullable public static final BlockType RED_MUSHROOM_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType RED_NETHER_BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType RED_NETHER_BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType RED_NETHER_BRICK_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType RED_NETHER_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType RED_SAND = BlockType.DUMMY;
    @Nullable public static final BlockType RED_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType RED_SANDSTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType RED_SANDSTONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType RED_SANDSTONE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType RED_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType RED_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType RED_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType RED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType RED_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType RED_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType RED_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType REDSTONE_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType REDSTONE_LAMP = BlockType.DUMMY;
    @Nullable public static final BlockType REDSTONE_ORE = BlockType.DUMMY;
    @Nullable public static final BlockType REDSTONE_TORCH = BlockType.DUMMY;
    @Nullable public static final BlockType REDSTONE_WALL_TORCH = BlockType.DUMMY;
    @Nullable public static final BlockType REDSTONE_WIRE = BlockType.DUMMY;
    @Nullable public static final BlockType REPEATER = BlockType.DUMMY;
    @Nullable public static final BlockType REPEATING_COMMAND_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType ROSE_BUSH = BlockType.DUMMY;
    @Nullable public static final BlockType SAND = BlockType.DUMMY;
    @Nullable public static final BlockType SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType SANDSTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType SANDSTONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType SANDSTONE_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType SCAFFOLDING = BlockType.DUMMY;
    @Nullable public static final BlockType SEA_LANTERN = BlockType.DUMMY;
    @Nullable public static final BlockType SEA_PICKLE = BlockType.DUMMY;
    @Nullable public static final BlockType SEAGRASS = BlockType.DUMMY;
    @Nullable public static final BlockType SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType SKELETON_SKULL = BlockType.DUMMY;
    @Nullable public static final BlockType SKELETON_WALL_SKULL = BlockType.DUMMY;
    @Nullable public static final BlockType SLIME_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType SMITHING_TABLE = BlockType.DUMMY;
    @Nullable public static final BlockType SMOKER = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_QUARTZ = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_QUARTZ_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_QUARTZ_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_RED_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_RED_SANDSTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_RED_SANDSTONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_SANDSTONE = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_SANDSTONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_SANDSTONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_STONE = BlockType.DUMMY;
    @Nullable public static final BlockType SMOOTH_STONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType SNOW = BlockType.DUMMY;
    @Nullable public static final BlockType SNOW_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType SOUL_SAND = BlockType.DUMMY;
    @Nullable public static final BlockType SPAWNER = BlockType.DUMMY;
    @Nullable public static final BlockType SPONGE = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_DOOR = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_FENCE = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_FENCE_GATE = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_LEAVES = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_PLANKS = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_SAPLING = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_TRAPDOOR = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_WALL_SIGN = BlockType.DUMMY;
    @Nullable public static final BlockType SPRUCE_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STICKY_PISTON = BlockType.DUMMY;
    @Nullable public static final BlockType STONE = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_BRICK_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_BRICK_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_BRICK_WALL = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_BRICKS = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_BUTTON = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_PRESSURE_PLATE = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_SLAB = BlockType.DUMMY;
    @Nullable public static final BlockType STONE_STAIRS = BlockType.DUMMY;
    @Nullable public static final BlockType STONECUTTER = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_ACACIA_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_ACACIA_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_BIRCH_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_BIRCH_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_DARK_OAK_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_DARK_OAK_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_JUNGLE_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_JUNGLE_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_OAK_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_OAK_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_SPRUCE_LOG = BlockType.DUMMY;
    @Nullable public static final BlockType STRIPPED_SPRUCE_WOOD = BlockType.DUMMY;
    @Nullable public static final BlockType STRUCTURE_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType STRUCTURE_VOID = BlockType.DUMMY;
    @Nullable public static final BlockType SUGAR_CANE = BlockType.DUMMY;
    @Nullable public static final BlockType SUNFLOWER = BlockType.DUMMY;
    @Nullable public static final BlockType SWEET_BERRY_BUSH = BlockType.DUMMY;
    @Nullable public static final BlockType TALL_GRASS = BlockType.DUMMY;
    @Nullable public static final BlockType TALL_SEAGRASS = BlockType.DUMMY;
    @Nullable public static final BlockType TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType TNT = BlockType.DUMMY;
    @Nullable public static final BlockType TORCH = BlockType.DUMMY;
    @Nullable public static final BlockType TRAPPED_CHEST = BlockType.DUMMY;
    @Nullable public static final BlockType TRIPWIRE = BlockType.DUMMY;
    @Nullable public static final BlockType TRIPWIRE_HOOK = BlockType.DUMMY;
    @Nullable public static final BlockType TUBE_CORAL = BlockType.DUMMY;
    @Nullable public static final BlockType TUBE_CORAL_BLOCK = BlockType.DUMMY;
    @Nullable public static final BlockType TUBE_CORAL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType TUBE_CORAL_WALL_FAN = BlockType.DUMMY;
    @Nullable public static final BlockType TURTLE_EGG = BlockType.DUMMY;
    @Nullable public static final BlockType VINE = BlockType.DUMMY;
    @Nullable public static final BlockType VOID_AIR = BlockType.DUMMY;
    @Nullable public static final BlockType WALL_TORCH = BlockType.DUMMY;
    @Nullable public static final BlockType WATER = BlockType.DUMMY;
    @Nullable public static final BlockType WET_SPONGE = BlockType.DUMMY;
    @Nullable public static final BlockType WHEAT = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_BED = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_TULIP = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType WHITE_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType WITHER_ROSE = BlockType.DUMMY;
    @Nullable public static final BlockType WITHER_SKELETON_SKULL = BlockType.DUMMY;
    @Nullable public static final BlockType WITHER_SKELETON_WALL_SKULL = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_BED = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_CARPET = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_CONCRETE = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_CONCRETE_POWDER = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_GLAZED_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_SHULKER_BOX = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_STAINED_GLASS = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_STAINED_GLASS_PANE = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_TERRACOTTA = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_WALL_BANNER = BlockType.DUMMY;
    @Nullable public static final BlockType YELLOW_WOOL = BlockType.DUMMY;
    @Nullable public static final BlockType ZOMBIE_HEAD = BlockType.DUMMY;
    @Nullable public static final BlockType ZOMBIE_WALL_HEAD = BlockType.DUMMY;

    // deprecated
    @Deprecated @Nullable public static BlockType SIGN;
    @Deprecated @Nullable public static BlockType WALL_SIGN;

    /*
     -----------------------------------------------------
                    Settings
     -----------------------------------------------------
     */
    protected final static class Settings {
        protected final int internalId;
        protected final BlockState defaultState;
        protected final AbstractProperty<?>[] propertiesMapArr;
        protected final AbstractProperty<?>[] propertiesArr;
        protected final List<AbstractProperty<?>> propertiesList;
        protected final Map<String, AbstractProperty<?>> propertiesMap;
        protected final Set<AbstractProperty<?>> propertiesSet;
        protected final BlockMaterial blockMaterial;
        protected final int permutations;
        protected int[] stateOrdinals;

        Settings(BlockType type, String id, int internalId, List<BlockState> states) {
            this.internalId = internalId;
            String propertyString = null;
            int propI = id.indexOf('[');
            if (propI != -1) {
                propertyString = id.substring(propI + 1, id.length() - 1);
            }

            int maxInternalStateId = 0;
            Map<String, ? extends Property<?>> properties = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getProperties(type);
            if (!properties.isEmpty()) {
                // Ensure the properties are registered
                int maxOrdinal = 0;
                for (String key : properties.keySet()) {
                    maxOrdinal = Math.max(PropertyKey.getOrCreate(key).ordinal(), maxOrdinal);
                }
                this.propertiesMapArr = new AbstractProperty[maxOrdinal + 1];
                int prop_arr_i = 0;
                this.propertiesArr = new AbstractProperty[properties.size()];
                HashMap<String, AbstractProperty<?>> propMap = new HashMap<>();

                int bitOffset = 0;
                for (Map.Entry<String, ? extends Property<?>> entry : properties.entrySet()) {
                    PropertyKey key = PropertyKey.getOrCreate(entry.getKey());
                    AbstractProperty<?> property = ((AbstractProperty) entry.getValue()).withOffset(bitOffset);
                    this.propertiesMapArr[key.ordinal()] = property;
                    this.propertiesArr[prop_arr_i++] = property;
                    propMap.put(entry.getKey(), property);

                    maxInternalStateId += (property.getValues().size() << bitOffset);
                    bitOffset += property.getNumBits();
                }
                this.propertiesList = Arrays.asList(this.propertiesArr);
                this.propertiesMap = Collections.unmodifiableMap(propMap);
                this.propertiesSet = new LinkedHashSet<>(this.propertiesMap.values());
            } else {
                this.propertiesMapArr = new AbstractProperty[0];
                this.propertiesArr = this.propertiesMapArr;
                this.propertiesList = Collections.emptyList();
                this.propertiesMap = Collections.emptyMap();
                this.propertiesSet = Collections.emptySet();
            }
            this.permutations = maxInternalStateId;

            this.blockMaterial = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getMaterial(type);

            if (!propertiesList.isEmpty()) {
                this.stateOrdinals = generateStateOrdinals(internalId, states.size(), maxInternalStateId, propertiesList);

                for (int propId = 0; propId < this.stateOrdinals.length; propId++) {
                    int ordinal = this.stateOrdinals[propId];
                    if (ordinal != -1) {
                        int stateId = internalId + (propId << BlockTypes.BIT_OFFSET);
                        BlockState state = new BlockState(type, stateId, ordinal);
                        states.add(state);
                    }
                }
                int defaultPropId = parseProperties(propertyString, propertiesMap) >> BlockTypes.BIT_OFFSET;

                this.defaultState = states.get(this.stateOrdinals[defaultPropId]);
            } else {
                this.defaultState = new BlockState(type, internalId, states.size());
                states.add(this.defaultState);
            }
        }

        private int parseProperties(String properties, Map<String, AbstractProperty<?>> propertyMap) {
            int id = internalId;
            for (String keyPair : properties.split(",")) {
                String[] split = keyPair.split("=");
                String name = split[0];
                String value = split[1];
                AbstractProperty btp = propertyMap.get(name);
                id = btp.modify(id, btp.getValueFor(value));
            }
            return id;
        }
    }


    private static int[] generateStateOrdinals(int internalId, int ordinal, int maxStateId, List<AbstractProperty<?>> props) {
        if (props.isEmpty()) return null;
        int[] result = new int[maxStateId];
        Arrays.fill(result, -1);
        int[] state = new int[props.size()];
        int[] sizes = new int[props.size()];
        for (int i = 0; i < props.size(); i++) {
            sizes[i] = props.get(i).getValues().size();
        }
        int index = 0;
        outer:
        while (true) {
            // Create the state
            int stateId = internalId;
            for (int i = 0; i < state.length; i++) {
                stateId = props.get(i).modifyIndex(stateId, state[i]);
            }
            // Map it to the ordinal
            result[stateId >> BlockTypes.BIT_OFFSET] = ordinal++;
            // Increment the state
            while (++state[index] == sizes[index]) {
                state[index] = 0;
                index++;
                if (index == state.length) break outer;
            }
            index = 0;
        }
        return result;
    }

    /*
     -----------------------------------------------------
                    Static Initializer
     -----------------------------------------------------
     */

    public static final int BIT_OFFSET; // Used internally
    protected static final int BIT_MASK; // Used internally

//    private static final Map<String, BlockType> $REGISTRY = new HashMap<>();
//    public static final NamespacedRegistry<BlockType> REGISTRY = new NamespacedRegistry<>("block type", $REGISTRY);

    public static final BlockType[] values;
    public static final BlockState[] states;

    private static final Set<String> $NAMESPACES = new LinkedHashSet<>();

    static {
        try {
            ArrayList<BlockState> stateList = new ArrayList<>();

            Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS);
            Registries registries = platform.getRegistries();
            BlockRegistry blockReg = registries.getBlockRegistry();
            Collection<String> blocks = blockReg.registerBlocks();
            Map<String, String> blockMap = blocks.stream().collect(Collectors.toMap(item -> item.charAt(item.length() - 1) == ']' ? item.substring(0, item.indexOf('[')) : item, item -> item));

            int size = blockMap.size();
            for (Field field : BlockID.class.getDeclaredFields()) size = Math.max(field.getInt(null) + 1, size);
            BIT_OFFSET = MathMan.log2nlz(size);
            BIT_MASK = ((1 << BIT_OFFSET) - 1);
            values = new BlockType[size];

            // Register the statically declared ones first
            Field[] oldFields = BlockID.class.getDeclaredFields();
            for (Field field : oldFields) {
                if (field.getType() == int.class) {
                    int internalId = field.getInt(null);
                    String id = "minecraft:" + field.getName().toLowerCase(Locale.ROOT);
                    String defaultState = blockMap.remove(id);
                    if (defaultState == null) {
                        if (internalId != 0) {
                            System.out.println("Ignoring invalid block " + id);
                            continue;
                        }
                        defaultState = id;
                    }
                    if (values[internalId] != null) {
                        throw new IllegalStateException("Invalid duplicate id for " + field.getName());
                    }
                    BlockType type = register(defaultState, internalId, stateList);
                    // Note: Throws IndexOutOfBoundsError if nothing is registered and blocksMap is empty
                    values[internalId] = type;
                }
            }

            { // Register new blocks
                int internalId = 1;
                for (Map.Entry<String, String> entry : blockMap.entrySet()) {
                    String defaultState = entry.getValue();
                    // Skip already registered ids
                    for (; values[internalId] != null; internalId++);
                    BlockType type = register(defaultState, internalId, stateList);
                    values[internalId] = type;
                }
            }

            states = stateList.toArray(new BlockState[stateList.size()]);


            // Init deprecated
            SIGN = OAK_SIGN;
            WALL_SIGN = OAK_WALL_SIGN;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static BlockType register(final String id, int internalId, List<BlockState> states) {
        // Get the enum name (remove namespace if minecraft:)
        int propStart = id.indexOf('[');
        String typeName = id.substring(0, propStart == -1 ? id.length() : propStart);
        String enumName = (typeName.startsWith("minecraft:") ? typeName.substring(10) : typeName).toUpperCase(Locale.ROOT);
        BlockType existing = new BlockType(id, internalId, states);


        // Set field value
        try {
            Field field = BlockTypes.class.getDeclaredField(enumName);
            ReflectionUtils.setFailsafeFieldValue(field, null, existing);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // register states
        BlockType.REGISTRY.register(typeName, existing);
        String nameSpace = typeName.substring(0, typeName.indexOf(':'));
        $NAMESPACES.add(nameSpace);
        return existing;
    }


    /*
     -----------------------------------------------------
                    Parsing
     -----------------------------------------------------
     */

    public static BlockType parse(final String type) throws InputParseException {
        final String inputLower = type.toLowerCase(Locale.ROOT);
        String input = inputLower;

        if (!input.split("\\[", 2)[0].contains(":")) input = "minecraft:" + input;
        BlockType result = BlockType.REGISTRY.get(input);
        if (result != null) return result;

        try {
            BlockStateHolder block = LegacyMapper.getInstance().getBlockFromLegacy(input);
            if (block != null) return block.getBlockType();
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
        }

        throw new SuggestInputParseException("Does not match a valid block type: " + inputLower, inputLower, () -> Stream.of(BlockTypes.values)
            .filter(b -> StringMan.blockStateMatches(inputLower, b.getId()))
            .map(BlockType::getId)
            .sorted(StringMan.blockStateComparator(inputLower))
            .collect(Collectors.toList())
        );
    }

    public static Set<String> getNameSpaces() {
        return $NAMESPACES;
    }

    @Nullable
    public static BlockType get(final String id) {
        return BlockType.REGISTRY.get(id);
    }

    @Nullable
    public static BlockType get(final CharSequence id) {
        return BlockType.REGISTRY.get(id.toString());
    }

    @Deprecated
    public static BlockType get(final int ordinal) {
        return values[ordinal];
    }

    @Deprecated
    public static BlockType getFromStateId(final int internalStateId) {
        return values[internalStateId & BIT_MASK];
    }

    @Deprecated
    public static BlockType getFromStateOrdinal(final int internalStateOrdinal) {
        return states[internalStateOrdinal].getBlockType();
    }

    public static int size() {
        return values.length;
    }

}
