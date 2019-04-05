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

import static com.google.common.base.Preconditions.checkArgument;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import it.unimi.dsi.fastutil.ints.IntCollections;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Stores a list of common Block String IDs.
 */
public final class BlockTypes {
    // Doesn't really matter what the hardcoded values are, as FAWE will update it on load
    @Nullable public static final BlockType __RESERVED__ = null;
    @Nullable public static final BlockType ACACIA_BUTTON = null;
    @Nullable public static final BlockType ACACIA_DOOR = null;
    @Nullable public static final BlockType ACACIA_FENCE = null;
    @Nullable public static final BlockType ACACIA_FENCE_GATE = null;
    @Nullable public static final BlockType ACACIA_LEAVES = null;
    @Nullable public static final BlockType ACACIA_LOG = null;
    @Nullable public static final BlockType ACACIA_PLANKS = null;
    @Nullable public static final BlockType ACACIA_PRESSURE_PLATE = null;
    @Nullable public static final BlockType ACACIA_SAPLING = null;
    @Nullable public static final BlockType ACACIA_SLAB = null;
    @Nullable public static final BlockType ACACIA_STAIRS = null;
    @Nullable public static final BlockType ACACIA_TRAPDOOR = null;
    @Nullable public static final BlockType ACACIA_WOOD = null;
    @Nullable public static final BlockType ACTIVATOR_RAIL = null;
    @Nullable public static final BlockType AIR = null;
    @Nullable public static final BlockType ALLIUM = null;
    @Nullable public static final BlockType ANDESITE = null;
    @Nullable public static final BlockType ANVIL = null;
    @Nullable public static final BlockType ATTACHED_MELON_STEM = null;
    @Nullable public static final BlockType ATTACHED_PUMPKIN_STEM = null;
    @Nullable public static final BlockType AZURE_BLUET = null;
    @Nullable public static final BlockType BARRIER = null;
    @Nullable public static final BlockType BEACON = null;
    @Nullable public static final BlockType BEDROCK = null;
    @Nullable public static final BlockType BEETROOTS = null;
    @Nullable public static final BlockType BIRCH_BUTTON = null;
    @Nullable public static final BlockType BIRCH_DOOR = null;
    @Nullable public static final BlockType BIRCH_FENCE = null;
    @Nullable public static final BlockType BIRCH_FENCE_GATE = null;
    @Nullable public static final BlockType BIRCH_LEAVES = null;
    @Nullable public static final BlockType BIRCH_LOG = null;
    @Nullable public static final BlockType BIRCH_PLANKS = null;
    @Nullable public static final BlockType BIRCH_PRESSURE_PLATE = null;
    @Nullable public static final BlockType BIRCH_SAPLING = null;
    @Nullable public static final BlockType BIRCH_SLAB = null;
    @Nullable public static final BlockType BIRCH_STAIRS = null;
    @Nullable public static final BlockType BIRCH_TRAPDOOR = null;
    @Nullable public static final BlockType BIRCH_WOOD = null;
    @Nullable public static final BlockType BLACK_BANNER = null;
    @Nullable public static final BlockType BLACK_BED = null;
    @Nullable public static final BlockType BLACK_CARPET = null;
    @Nullable public static final BlockType BLACK_CONCRETE = null;
    @Nullable public static final BlockType BLACK_CONCRETE_POWDER = null;
    @Nullable public static final BlockType BLACK_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType BLACK_SHULKER_BOX = null;
    @Nullable public static final BlockType BLACK_STAINED_GLASS = null;
    @Nullable public static final BlockType BLACK_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType BLACK_TERRACOTTA = null;
    @Nullable public static final BlockType BLACK_WALL_BANNER = null;
    @Nullable public static final BlockType BLACK_WOOL = null;
    @Nullable public static final BlockType BLUE_BANNER = null;
    @Nullable public static final BlockType BLUE_BED = null;
    @Nullable public static final BlockType BLUE_CARPET = null;
    @Nullable public static final BlockType BLUE_CONCRETE = null;
    @Nullable public static final BlockType BLUE_CONCRETE_POWDER = null;
    @Nullable public static final BlockType BLUE_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType BLUE_ICE = null;
    @Nullable public static final BlockType BLUE_ORCHID = null;
    @Nullable public static final BlockType BLUE_SHULKER_BOX = null;
    @Nullable public static final BlockType BLUE_STAINED_GLASS = null;
    @Nullable public static final BlockType BLUE_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType BLUE_TERRACOTTA = null;
    @Nullable public static final BlockType BLUE_WALL_BANNER = null;
    @Nullable public static final BlockType BLUE_WOOL = null;
    @Nullable public static final BlockType BONE_BLOCK = null;
    @Nullable public static final BlockType BOOKSHELF = null;
    @Nullable public static final BlockType BRAIN_CORAL = null;
    @Nullable public static final BlockType BRAIN_CORAL_BLOCK = null;
    @Nullable public static final BlockType BRAIN_CORAL_FAN = null;
    @Nullable public static final BlockType BRAIN_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType BREWING_STAND = null;
    @Nullable public static final BlockType BRICK_SLAB = null;
    @Nullable public static final BlockType BRICK_STAIRS = null;
    @Nullable public static final BlockType BRICKS = null;
    @Nullable public static final BlockType BROWN_BANNER = null;
    @Nullable public static final BlockType BROWN_BED = null;
    @Nullable public static final BlockType BROWN_CARPET = null;
    @Nullable public static final BlockType BROWN_CONCRETE = null;
    @Nullable public static final BlockType BROWN_CONCRETE_POWDER = null;
    @Nullable public static final BlockType BROWN_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType BROWN_MUSHROOM = null;
    @Nullable public static final BlockType BROWN_MUSHROOM_BLOCK = null;
    @Nullable public static final BlockType BROWN_SHULKER_BOX = null;
    @Nullable public static final BlockType BROWN_STAINED_GLASS = null;
    @Nullable public static final BlockType BROWN_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType BROWN_TERRACOTTA = null;
    @Nullable public static final BlockType BROWN_WALL_BANNER = null;
    @Nullable public static final BlockType BROWN_WOOL = null;
    @Nullable public static final BlockType BUBBLE_COLUMN = null;
    @Nullable public static final BlockType BUBBLE_CORAL = null;
    @Nullable public static final BlockType BUBBLE_CORAL_BLOCK = null;
    @Nullable public static final BlockType BUBBLE_CORAL_FAN = null;
    @Nullable public static final BlockType BUBBLE_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType CACTUS = null;
    @Nullable public static final BlockType CAKE = null;
    @Nullable public static final BlockType CARROTS = null;
    @Nullable public static final BlockType CARVED_PUMPKIN = null;
    @Nullable public static final BlockType CAULDRON = null;
    @Nullable public static final BlockType CAVE_AIR = null;
    @Nullable public static final BlockType CHAIN_COMMAND_BLOCK = null;
    @Nullable public static final BlockType CHEST = null;
    @Nullable public static final BlockType CHIPPED_ANVIL = null;
    @Nullable public static final BlockType CHISELED_QUARTZ_BLOCK = null;
    @Nullable public static final BlockType CHISELED_RED_SANDSTONE = null;
    @Nullable public static final BlockType CHISELED_SANDSTONE = null;
    @Nullable public static final BlockType CHISELED_STONE_BRICKS = null;
    @Nullable public static final BlockType CHORUS_FLOWER = null;
    @Nullable public static final BlockType CHORUS_PLANT = null;
    @Nullable public static final BlockType CLAY = null;
    @Nullable public static final BlockType COAL_BLOCK = null;
    @Nullable public static final BlockType COAL_ORE = null;
    @Nullable public static final BlockType COARSE_DIRT = null;
    @Nullable public static final BlockType COBBLESTONE = null;
    @Nullable public static final BlockType COBBLESTONE_SLAB = null;
    @Nullable public static final BlockType COBBLESTONE_STAIRS = null;
    @Nullable public static final BlockType COBBLESTONE_WALL = null;
    @Nullable public static final BlockType COBWEB = null;
    @Nullable public static final BlockType COCOA = null;
    @Nullable public static final BlockType COMMAND_BLOCK = null;
    @Nullable public static final BlockType COMPARATOR = null;
    @Nullable public static final BlockType CONDUIT = null;
    @Nullable public static final BlockType CRACKED_STONE_BRICKS = null;
    @Nullable public static final BlockType CRAFTING_TABLE = null;
    @Nullable public static final BlockType CREEPER_HEAD = null;
    @Nullable public static final BlockType CREEPER_WALL_HEAD = null;
    @Nullable public static final BlockType CUT_RED_SANDSTONE = null;
    @Nullable public static final BlockType CUT_SANDSTONE = null;
    @Nullable public static final BlockType CYAN_BANNER = null;
    @Nullable public static final BlockType CYAN_BED = null;
    @Nullable public static final BlockType CYAN_CARPET = null;
    @Nullable public static final BlockType CYAN_CONCRETE = null;
    @Nullable public static final BlockType CYAN_CONCRETE_POWDER = null;
    @Nullable public static final BlockType CYAN_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType CYAN_SHULKER_BOX = null;
    @Nullable public static final BlockType CYAN_STAINED_GLASS = null;
    @Nullable public static final BlockType CYAN_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType CYAN_TERRACOTTA = null;
    @Nullable public static final BlockType CYAN_WALL_BANNER = null;
    @Nullable public static final BlockType CYAN_WOOL = null;
    @Nullable public static final BlockType DAMAGED_ANVIL = null;
    @Nullable public static final BlockType DANDELION = null;
    @Nullable public static final BlockType DARK_OAK_BUTTON = null;
    @Nullable public static final BlockType DARK_OAK_DOOR = null;
    @Nullable public static final BlockType DARK_OAK_FENCE = null;
    @Nullable public static final BlockType DARK_OAK_FENCE_GATE = null;
    @Nullable public static final BlockType DARK_OAK_LEAVES = null;
    @Nullable public static final BlockType DARK_OAK_LOG = null;
    @Nullable public static final BlockType DARK_OAK_PLANKS = null;
    @Nullable public static final BlockType DARK_OAK_PRESSURE_PLATE = null;
    @Nullable public static final BlockType DARK_OAK_SAPLING = null;
    @Nullable public static final BlockType DARK_OAK_SLAB = null;
    @Nullable public static final BlockType DARK_OAK_STAIRS = null;
    @Nullable public static final BlockType DARK_OAK_TRAPDOOR = null;
    @Nullable public static final BlockType DARK_OAK_WOOD = null;
    @Nullable public static final BlockType DARK_PRISMARINE = null;
    @Nullable public static final BlockType DARK_PRISMARINE_SLAB = null;
    @Nullable public static final BlockType DARK_PRISMARINE_STAIRS = null;
    @Nullable public static final BlockType DAYLIGHT_DETECTOR = null;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL = null;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL_BLOCK = null;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL_FAN = null;
    @Nullable public static final BlockType DEAD_BRAIN_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL = null;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL_BLOCK = null;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL_FAN = null;
    @Nullable public static final BlockType DEAD_BUBBLE_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType DEAD_BUSH = null;
    @Nullable public static final BlockType DEAD_FIRE_CORAL = null;
    @Nullable public static final BlockType DEAD_FIRE_CORAL_BLOCK = null;
    @Nullable public static final BlockType DEAD_FIRE_CORAL_FAN = null;
    @Nullable public static final BlockType DEAD_FIRE_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType DEAD_HORN_CORAL = null;
    @Nullable public static final BlockType DEAD_HORN_CORAL_BLOCK = null;
    @Nullable public static final BlockType DEAD_HORN_CORAL_FAN = null;
    @Nullable public static final BlockType DEAD_HORN_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType DEAD_TUBE_CORAL = null;
    @Nullable public static final BlockType DEAD_TUBE_CORAL_BLOCK = null;
    @Nullable public static final BlockType DEAD_TUBE_CORAL_FAN = null;
    @Nullable public static final BlockType DEAD_TUBE_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType DETECTOR_RAIL = null;
    @Nullable public static final BlockType DIAMOND_BLOCK = null;
    @Nullable public static final BlockType DIAMOND_ORE = null;
    @Nullable public static final BlockType DIORITE = null;
    @Nullable public static final BlockType DIRT = null;
    @Nullable public static final BlockType DISPENSER = null;
    @Nullable public static final BlockType DRAGON_EGG = null;
    @Nullable public static final BlockType DRAGON_HEAD = null;
    @Nullable public static final BlockType DRAGON_WALL_HEAD = null;
    @Nullable public static final BlockType DRIED_KELP_BLOCK = null;
    @Nullable public static final BlockType DROPPER = null;
    @Nullable public static final BlockType EMERALD_BLOCK = null;
    @Nullable public static final BlockType EMERALD_ORE = null;
    @Nullable public static final BlockType ENCHANTING_TABLE = null;
    @Nullable public static final BlockType END_GATEWAY = null;
    @Nullable public static final BlockType END_PORTAL = null;
    @Nullable public static final BlockType END_PORTAL_FRAME = null;
    @Nullable public static final BlockType END_ROD = null;
    @Nullable public static final BlockType END_STONE = null;
    @Nullable public static final BlockType END_STONE_BRICKS = null;
    @Nullable public static final BlockType ENDER_CHEST = null;
    @Nullable public static final BlockType FARMLAND = null;
    @Nullable public static final BlockType FERN = null;
    @Nullable public static final BlockType FIRE = null;
    @Nullable public static final BlockType FIRE_CORAL = null;
    @Nullable public static final BlockType FIRE_CORAL_BLOCK = null;
    @Nullable public static final BlockType FIRE_CORAL_FAN = null;
    @Nullable public static final BlockType FIRE_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType FLOWER_POT = null;
    @Nullable public static final BlockType FROSTED_ICE = null;
    @Nullable public static final BlockType FURNACE = null;
    @Nullable public static final BlockType GLASS = null;
    @Nullable public static final BlockType GLASS_PANE = null;
    @Nullable public static final BlockType GLOWSTONE = null;
    @Nullable public static final BlockType GOLD_BLOCK = null;
    @Nullable public static final BlockType GOLD_ORE = null;
    @Nullable public static final BlockType GRANITE = null;
    @Nullable public static final BlockType GRASS = null;
    @Nullable public static final BlockType GRASS_BLOCK = null;
    @Nullable public static final BlockType GRASS_PATH = null;
    @Nullable public static final BlockType GRAVEL = null;
    @Nullable public static final BlockType GRAY_BANNER = null;
    @Nullable public static final BlockType GRAY_BED = null;
    @Nullable public static final BlockType GRAY_CARPET = null;
    @Nullable public static final BlockType GRAY_CONCRETE = null;
    @Nullable public static final BlockType GRAY_CONCRETE_POWDER = null;
    @Nullable public static final BlockType GRAY_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType GRAY_SHULKER_BOX = null;
    @Nullable public static final BlockType GRAY_STAINED_GLASS = null;
    @Nullable public static final BlockType GRAY_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType GRAY_TERRACOTTA = null;
    @Nullable public static final BlockType GRAY_WALL_BANNER = null;
    @Nullable public static final BlockType GRAY_WOOL = null;
    @Nullable public static final BlockType GREEN_BANNER = null;
    @Nullable public static final BlockType GREEN_BED = null;
    @Nullable public static final BlockType GREEN_CARPET = null;
    @Nullable public static final BlockType GREEN_CONCRETE = null;
    @Nullable public static final BlockType GREEN_CONCRETE_POWDER = null;
    @Nullable public static final BlockType GREEN_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType GREEN_SHULKER_BOX = null;
    @Nullable public static final BlockType GREEN_STAINED_GLASS = null;
    @Nullable public static final BlockType GREEN_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType GREEN_TERRACOTTA = null;
    @Nullable public static final BlockType GREEN_WALL_BANNER = null;
    @Nullable public static final BlockType GREEN_WOOL = null;
    @Nullable public static final BlockType HAY_BLOCK = null;
    @Nullable public static final BlockType HEAVY_WEIGHTED_PRESSURE_PLATE = null;
    @Nullable public static final BlockType HOPPER = null;
    @Nullable public static final BlockType HORN_CORAL = null;
    @Nullable public static final BlockType HORN_CORAL_BLOCK = null;
    @Nullable public static final BlockType HORN_CORAL_FAN = null;
    @Nullable public static final BlockType HORN_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType ICE = null;
    @Nullable public static final BlockType INFESTED_CHISELED_STONE_BRICKS = null;
    @Nullable public static final BlockType INFESTED_COBBLESTONE = null;
    @Nullable public static final BlockType INFESTED_CRACKED_STONE_BRICKS = null;
    @Nullable public static final BlockType INFESTED_MOSSY_STONE_BRICKS = null;
    @Nullable public static final BlockType INFESTED_STONE = null;
    @Nullable public static final BlockType INFESTED_STONE_BRICKS = null;
    @Nullable public static final BlockType IRON_BARS = null;
    @Nullable public static final BlockType IRON_BLOCK = null;
    @Nullable public static final BlockType IRON_DOOR = null;
    @Nullable public static final BlockType IRON_ORE = null;
    @Nullable public static final BlockType IRON_TRAPDOOR = null;
    @Nullable public static final BlockType JACK_O_LANTERN = null;
    @Nullable public static final BlockType JUKEBOX = null;
    @Nullable public static final BlockType JUNGLE_BUTTON = null;
    @Nullable public static final BlockType JUNGLE_DOOR = null;
    @Nullable public static final BlockType JUNGLE_FENCE = null;
    @Nullable public static final BlockType JUNGLE_FENCE_GATE = null;
    @Nullable public static final BlockType JUNGLE_LEAVES = null;
    @Nullable public static final BlockType JUNGLE_LOG = null;
    @Nullable public static final BlockType JUNGLE_PLANKS = null;
    @Nullable public static final BlockType JUNGLE_PRESSURE_PLATE = null;
    @Nullable public static final BlockType JUNGLE_SAPLING = null;
    @Nullable public static final BlockType JUNGLE_SLAB = null;
    @Nullable public static final BlockType JUNGLE_STAIRS = null;
    @Nullable public static final BlockType JUNGLE_TRAPDOOR = null;
    @Nullable public static final BlockType JUNGLE_WOOD = null;
    @Nullable public static final BlockType KELP = null;
    @Nullable public static final BlockType KELP_PLANT = null;
    @Nullable public static final BlockType LADDER = null;
    @Nullable public static final BlockType LAPIS_BLOCK = null;
    @Nullable public static final BlockType LAPIS_ORE = null;
    @Nullable public static final BlockType LARGE_FERN = null;
    @Nullable public static final BlockType LAVA = null;
    @Nullable public static final BlockType LEVER = null;
    @Nullable public static final BlockType LIGHT_BLUE_BANNER = null;
    @Nullable public static final BlockType LIGHT_BLUE_BED = null;
    @Nullable public static final BlockType LIGHT_BLUE_CARPET = null;
    @Nullable public static final BlockType LIGHT_BLUE_CONCRETE = null;
    @Nullable public static final BlockType LIGHT_BLUE_CONCRETE_POWDER = null;
    @Nullable public static final BlockType LIGHT_BLUE_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType LIGHT_BLUE_SHULKER_BOX = null;
    @Nullable public static final BlockType LIGHT_BLUE_STAINED_GLASS = null;
    @Nullable public static final BlockType LIGHT_BLUE_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType LIGHT_BLUE_TERRACOTTA = null;
    @Nullable public static final BlockType LIGHT_BLUE_WALL_BANNER = null;
    @Nullable public static final BlockType LIGHT_BLUE_WOOL = null;
    @Nullable public static final BlockType LIGHT_GRAY_BANNER = null;
    @Nullable public static final BlockType LIGHT_GRAY_BED = null;
    @Nullable public static final BlockType LIGHT_GRAY_CARPET = null;
    @Nullable public static final BlockType LIGHT_GRAY_CONCRETE = null;
    @Nullable public static final BlockType LIGHT_GRAY_CONCRETE_POWDER = null;
    @Nullable public static final BlockType LIGHT_GRAY_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType LIGHT_GRAY_SHULKER_BOX = null;
    @Nullable public static final BlockType LIGHT_GRAY_STAINED_GLASS = null;
    @Nullable public static final BlockType LIGHT_GRAY_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType LIGHT_GRAY_TERRACOTTA = null;
    @Nullable public static final BlockType LIGHT_GRAY_WALL_BANNER = null;
    @Nullable public static final BlockType LIGHT_GRAY_WOOL = null;
    @Nullable public static final BlockType LIGHT_WEIGHTED_PRESSURE_PLATE = null;
    @Nullable public static final BlockType LILAC = null;
    @Nullable public static final BlockType LILY_PAD = null;
    @Nullable public static final BlockType LIME_BANNER = null;
    @Nullable public static final BlockType LIME_BED = null;
    @Nullable public static final BlockType LIME_CARPET = null;
    @Nullable public static final BlockType LIME_CONCRETE = null;
    @Nullable public static final BlockType LIME_CONCRETE_POWDER = null;
    @Nullable public static final BlockType LIME_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType LIME_SHULKER_BOX = null;
    @Nullable public static final BlockType LIME_STAINED_GLASS = null;
    @Nullable public static final BlockType LIME_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType LIME_TERRACOTTA = null;
    @Nullable public static final BlockType LIME_WALL_BANNER = null;
    @Nullable public static final BlockType LIME_WOOL = null;
    @Nullable public static final BlockType MAGENTA_BANNER = null;
    @Nullable public static final BlockType MAGENTA_BED = null;
    @Nullable public static final BlockType MAGENTA_CARPET = null;
    @Nullable public static final BlockType MAGENTA_CONCRETE = null;
    @Nullable public static final BlockType MAGENTA_CONCRETE_POWDER = null;
    @Nullable public static final BlockType MAGENTA_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType MAGENTA_SHULKER_BOX = null;
    @Nullable public static final BlockType MAGENTA_STAINED_GLASS = null;
    @Nullable public static final BlockType MAGENTA_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType MAGENTA_TERRACOTTA = null;
    @Nullable public static final BlockType MAGENTA_WALL_BANNER = null;
    @Nullable public static final BlockType MAGENTA_WOOL = null;
    @Nullable public static final BlockType MAGMA_BLOCK = null;
    @Nullable public static final BlockType MELON = null;
    @Nullable public static final BlockType MELON_STEM = null;
    @Nullable public static final BlockType MOSSY_COBBLESTONE = null;
    @Nullable public static final BlockType MOSSY_COBBLESTONE_WALL = null;
    @Nullable public static final BlockType MOSSY_STONE_BRICKS = null;
    @Nullable public static final BlockType MOVING_PISTON = null;
    @Nullable public static final BlockType MUSHROOM_STEM = null;
    @Nullable public static final BlockType MYCELIUM = null;
    @Nullable public static final BlockType NETHER_BRICK_FENCE = null;
    @Nullable public static final BlockType NETHER_BRICK_SLAB = null;
    @Nullable public static final BlockType NETHER_BRICK_STAIRS = null;
    @Nullable public static final BlockType NETHER_BRICKS = null;
    @Nullable public static final BlockType NETHER_PORTAL = null;
    @Nullable public static final BlockType NETHER_QUARTZ_ORE = null;
    @Nullable public static final BlockType NETHER_WART = null;
    @Nullable public static final BlockType NETHER_WART_BLOCK = null;
    @Nullable public static final BlockType NETHERRACK = null;
    @Nullable public static final BlockType NOTE_BLOCK = null;
    @Nullable public static final BlockType OAK_BUTTON = null;
    @Nullable public static final BlockType OAK_DOOR = null;
    @Nullable public static final BlockType OAK_FENCE = null;
    @Nullable public static final BlockType OAK_FENCE_GATE = null;
    @Nullable public static final BlockType OAK_LEAVES = null;
    @Nullable public static final BlockType OAK_LOG = null;
    @Nullable public static final BlockType OAK_PLANKS = null;
    @Nullable public static final BlockType OAK_PRESSURE_PLATE = null;
    @Nullable public static final BlockType OAK_SAPLING = null;
    @Nullable public static final BlockType OAK_SLAB = null;
    @Nullable public static final BlockType OAK_STAIRS = null;
    @Nullable public static final BlockType OAK_TRAPDOOR = null;
    @Nullable public static final BlockType OAK_WOOD = null;
    @Nullable public static final BlockType OBSERVER = null;
    @Nullable public static final BlockType OBSIDIAN = null;
    @Nullable public static final BlockType ORANGE_BANNER = null;
    @Nullable public static final BlockType ORANGE_BED = null;
    @Nullable public static final BlockType ORANGE_CARPET = null;
    @Nullable public static final BlockType ORANGE_CONCRETE = null;
    @Nullable public static final BlockType ORANGE_CONCRETE_POWDER = null;
    @Nullable public static final BlockType ORANGE_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType ORANGE_SHULKER_BOX = null;
    @Nullable public static final BlockType ORANGE_STAINED_GLASS = null;
    @Nullable public static final BlockType ORANGE_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType ORANGE_TERRACOTTA = null;
    @Nullable public static final BlockType ORANGE_TULIP = null;
    @Nullable public static final BlockType ORANGE_WALL_BANNER = null;
    @Nullable public static final BlockType ORANGE_WOOL = null;
    @Nullable public static final BlockType OXEYE_DAISY = null;
    @Nullable public static final BlockType PACKED_ICE = null;
    @Nullable public static final BlockType PEONY = null;
    @Nullable public static final BlockType PETRIFIED_OAK_SLAB = null;
    @Nullable public static final BlockType PINK_BANNER = null;
    @Nullable public static final BlockType PINK_BED = null;
    @Nullable public static final BlockType PINK_CARPET = null;
    @Nullable public static final BlockType PINK_CONCRETE = null;
    @Nullable public static final BlockType PINK_CONCRETE_POWDER = null;
    @Nullable public static final BlockType PINK_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType PINK_SHULKER_BOX = null;
    @Nullable public static final BlockType PINK_STAINED_GLASS = null;
    @Nullable public static final BlockType PINK_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType PINK_TERRACOTTA = null;
    @Nullable public static final BlockType PINK_TULIP = null;
    @Nullable public static final BlockType PINK_WALL_BANNER = null;
    @Nullable public static final BlockType PINK_WOOL = null;
    @Nullable public static final BlockType PISTON = null;
    @Nullable public static final BlockType PISTON_HEAD = null;
    @Nullable public static final BlockType PLAYER_HEAD = null;
    @Nullable public static final BlockType PLAYER_WALL_HEAD = null;
    @Nullable public static final BlockType PODZOL = null;
    @Nullable public static final BlockType POLISHED_ANDESITE = null;
    @Nullable public static final BlockType POLISHED_DIORITE = null;
    @Nullable public static final BlockType POLISHED_GRANITE = null;
    @Nullable public static final BlockType POPPY = null;
    @Nullable public static final BlockType POTATOES = null;
    @Nullable public static final BlockType POTTED_ACACIA_SAPLING = null;
    @Nullable public static final BlockType POTTED_ALLIUM = null;
    @Nullable public static final BlockType POTTED_AZURE_BLUET = null;
    @Nullable public static final BlockType POTTED_BIRCH_SAPLING = null;
    @Nullable public static final BlockType POTTED_BLUE_ORCHID = null;
    @Nullable public static final BlockType POTTED_BROWN_MUSHROOM = null;
    @Nullable public static final BlockType POTTED_CACTUS = null;
    @Nullable public static final BlockType POTTED_DANDELION = null;
    @Nullable public static final BlockType POTTED_DARK_OAK_SAPLING = null;
    @Nullable public static final BlockType POTTED_DEAD_BUSH = null;
    @Nullable public static final BlockType POTTED_FERN = null;
    @Nullable public static final BlockType POTTED_JUNGLE_SAPLING = null;
    @Nullable public static final BlockType POTTED_OAK_SAPLING = null;
    @Nullable public static final BlockType POTTED_ORANGE_TULIP = null;
    @Nullable public static final BlockType POTTED_OXEYE_DAISY = null;
    @Nullable public static final BlockType POTTED_PINK_TULIP = null;
    @Nullable public static final BlockType POTTED_POPPY = null;
    @Nullable public static final BlockType POTTED_RED_MUSHROOM = null;
    @Nullable public static final BlockType POTTED_RED_TULIP = null;
    @Nullable public static final BlockType POTTED_SPRUCE_SAPLING = null;
    @Nullable public static final BlockType POTTED_WHITE_TULIP = null;
    @Nullable public static final BlockType POWERED_RAIL = null;
    @Nullable public static final BlockType PRISMARINE = null;
    @Nullable public static final BlockType PRISMARINE_BRICK_SLAB = null;
    @Nullable public static final BlockType PRISMARINE_BRICK_STAIRS = null;
    @Nullable public static final BlockType PRISMARINE_BRICKS = null;
    @Nullable public static final BlockType PRISMARINE_SLAB = null;
    @Nullable public static final BlockType PRISMARINE_STAIRS = null;
    @Nullable public static final BlockType PUMPKIN = null;
    @Nullable public static final BlockType PUMPKIN_STEM = null;
    @Nullable public static final BlockType PURPLE_BANNER = null;
    @Nullable public static final BlockType PURPLE_BED = null;
    @Nullable public static final BlockType PURPLE_CARPET = null;
    @Nullable public static final BlockType PURPLE_CONCRETE = null;
    @Nullable public static final BlockType PURPLE_CONCRETE_POWDER = null;
    @Nullable public static final BlockType PURPLE_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType PURPLE_SHULKER_BOX = null;
    @Nullable public static final BlockType PURPLE_STAINED_GLASS = null;
    @Nullable public static final BlockType PURPLE_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType PURPLE_TERRACOTTA = null;
    @Nullable public static final BlockType PURPLE_WALL_BANNER = null;
    @Nullable public static final BlockType PURPLE_WOOL = null;
    @Nullable public static final BlockType PURPUR_BLOCK = null;
    @Nullable public static final BlockType PURPUR_PILLAR = null;
    @Nullable public static final BlockType PURPUR_SLAB = null;
    @Nullable public static final BlockType PURPUR_STAIRS = null;
    @Nullable public static final BlockType QUARTZ_BLOCK = null;
    @Nullable public static final BlockType QUARTZ_PILLAR = null;
    @Nullable public static final BlockType QUARTZ_SLAB = null;
    @Nullable public static final BlockType QUARTZ_STAIRS = null;
    @Nullable public static final BlockType RAIL = null;
    @Nullable public static final BlockType RED_BANNER = null;
    @Nullable public static final BlockType RED_BED = null;
    @Nullable public static final BlockType RED_CARPET = null;
    @Nullable public static final BlockType RED_CONCRETE = null;
    @Nullable public static final BlockType RED_CONCRETE_POWDER = null;
    @Nullable public static final BlockType RED_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType RED_MUSHROOM = null;
    @Nullable public static final BlockType RED_MUSHROOM_BLOCK = null;
    @Nullable public static final BlockType RED_NETHER_BRICKS = null;
    @Nullable public static final BlockType RED_SAND = null;
    @Nullable public static final BlockType RED_SANDSTONE = null;
    @Nullable public static final BlockType RED_SANDSTONE_SLAB = null;
    @Nullable public static final BlockType RED_SANDSTONE_STAIRS = null;
    @Nullable public static final BlockType RED_SHULKER_BOX = null;
    @Nullable public static final BlockType RED_STAINED_GLASS = null;
    @Nullable public static final BlockType RED_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType RED_TERRACOTTA = null;
    @Nullable public static final BlockType RED_TULIP = null;
    @Nullable public static final BlockType RED_WALL_BANNER = null;
    @Nullable public static final BlockType RED_WOOL = null;
    @Nullable public static final BlockType REDSTONE_BLOCK = null;
    @Nullable public static final BlockType REDSTONE_LAMP = null;
    @Nullable public static final BlockType REDSTONE_ORE = null;
    @Nullable public static final BlockType REDSTONE_TORCH = null;
    @Nullable public static final BlockType REDSTONE_WALL_TORCH = null;
    @Nullable public static final BlockType REDSTONE_WIRE = null;
    @Nullable public static final BlockType REPEATER = null;
    @Nullable public static final BlockType REPEATING_COMMAND_BLOCK = null;
    @Nullable public static final BlockType ROSE_BUSH = null;
    @Nullable public static final BlockType SAND = null;
    @Nullable public static final BlockType SANDSTONE = null;
    @Nullable public static final BlockType SANDSTONE_SLAB = null;
    @Nullable public static final BlockType SANDSTONE_STAIRS = null;
    @Nullable public static final BlockType SEA_LANTERN = null;
    @Nullable public static final BlockType SEA_PICKLE = null;
    @Nullable public static final BlockType SEAGRASS = null;
    @Nullable public static final BlockType SHULKER_BOX = null;
    @Nullable public static final BlockType SIGN = null;
    @Nullable public static final BlockType SKELETON_SKULL = null;
    @Nullable public static final BlockType SKELETON_WALL_SKULL = null;
    @Nullable public static final BlockType SLIME_BLOCK = null;
    @Nullable public static final BlockType SMOOTH_QUARTZ = null;
    @Nullable public static final BlockType SMOOTH_RED_SANDSTONE = null;
    @Nullable public static final BlockType SMOOTH_SANDSTONE = null;
    @Nullable public static final BlockType SMOOTH_STONE = null;
    @Nullable public static final BlockType SNOW = null;
    @Nullable public static final BlockType SNOW_BLOCK = null;
    @Nullable public static final BlockType SOUL_SAND = null;
    @Nullable public static final BlockType SPAWNER = null;
    @Nullable public static final BlockType SPONGE = null;
    @Nullable public static final BlockType SPRUCE_BUTTON = null;
    @Nullable public static final BlockType SPRUCE_DOOR = null;
    @Nullable public static final BlockType SPRUCE_FENCE = null;
    @Nullable public static final BlockType SPRUCE_FENCE_GATE = null;
    @Nullable public static final BlockType SPRUCE_LEAVES = null;
    @Nullable public static final BlockType SPRUCE_LOG = null;
    @Nullable public static final BlockType SPRUCE_PLANKS = null;
    @Nullable public static final BlockType SPRUCE_PRESSURE_PLATE = null;
    @Nullable public static final BlockType SPRUCE_SAPLING = null;
    @Nullable public static final BlockType SPRUCE_SLAB = null;
    @Nullable public static final BlockType SPRUCE_STAIRS = null;
    @Nullable public static final BlockType SPRUCE_TRAPDOOR = null;
    @Nullable public static final BlockType SPRUCE_WOOD = null;
    @Nullable public static final BlockType STICKY_PISTON = null;
    @Nullable public static final BlockType STONE = null;
    @Nullable public static final BlockType STONE_BRICK_SLAB = null;
    @Nullable public static final BlockType STONE_BRICK_STAIRS = null;
    @Nullable public static final BlockType STONE_BRICKS = null;
    @Nullable public static final BlockType STONE_BUTTON = null;
    @Nullable public static final BlockType STONE_PRESSURE_PLATE = null;
    @Nullable public static final BlockType STONE_SLAB = null;
    @Nullable public static final BlockType STRIPPED_ACACIA_LOG = null;
    @Nullable public static final BlockType STRIPPED_ACACIA_WOOD = null;
    @Nullable public static final BlockType STRIPPED_BIRCH_LOG = null;
    @Nullable public static final BlockType STRIPPED_BIRCH_WOOD = null;
    @Nullable public static final BlockType STRIPPED_DARK_OAK_LOG = null;
    @Nullable public static final BlockType STRIPPED_DARK_OAK_WOOD = null;
    @Nullable public static final BlockType STRIPPED_JUNGLE_LOG = null;
    @Nullable public static final BlockType STRIPPED_JUNGLE_WOOD = null;
    @Nullable public static final BlockType STRIPPED_OAK_LOG = null;
    @Nullable public static final BlockType STRIPPED_OAK_WOOD = null;
    @Nullable public static final BlockType STRIPPED_SPRUCE_LOG = null;
    @Nullable public static final BlockType STRIPPED_SPRUCE_WOOD = null;
    @Nullable public static final BlockType STRUCTURE_BLOCK = null;
    @Nullable public static final BlockType STRUCTURE_VOID = null;
    @Nullable public static final BlockType SUGAR_CANE = null;
    @Nullable public static final BlockType SUNFLOWER = null;
    @Nullable public static final BlockType TALL_GRASS = null;
    @Nullable public static final BlockType TALL_SEAGRASS = null;
    @Nullable public static final BlockType TERRACOTTA = null;
    @Nullable public static final BlockType TNT = null;
    @Nullable public static final BlockType TORCH = null;
    @Nullable public static final BlockType TRAPPED_CHEST = null;
    @Nullable public static final BlockType TRIPWIRE = null;
    @Nullable public static final BlockType TRIPWIRE_HOOK = null;
    @Nullable public static final BlockType TUBE_CORAL = null;
    @Nullable public static final BlockType TUBE_CORAL_BLOCK = null;
    @Nullable public static final BlockType TUBE_CORAL_FAN = null;
    @Nullable public static final BlockType TUBE_CORAL_WALL_FAN = null;
    @Nullable public static final BlockType TURTLE_EGG = null;
    @Nullable public static final BlockType VINE = null;
    @Nullable public static final BlockType VOID_AIR = null;
    @Nullable public static final BlockType WALL_SIGN = null;
    @Nullable public static final BlockType WALL_TORCH = null;
    @Nullable public static final BlockType WATER = null;
    @Nullable public static final BlockType WET_SPONGE = null;
    @Nullable public static final BlockType WHEAT = null;
    @Nullable public static final BlockType WHITE_BANNER = null;
    @Nullable public static final BlockType WHITE_BED = null;
    @Nullable public static final BlockType WHITE_CARPET = null;
    @Nullable public static final BlockType WHITE_CONCRETE = null;
    @Nullable public static final BlockType WHITE_CONCRETE_POWDER = null;
    @Nullable public static final BlockType WHITE_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType WHITE_SHULKER_BOX = null;
    @Nullable public static final BlockType WHITE_STAINED_GLASS = null;
    @Nullable public static final BlockType WHITE_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType WHITE_TERRACOTTA = null;
    @Nullable public static final BlockType WHITE_TULIP = null;
    @Nullable public static final BlockType WHITE_WALL_BANNER = null;
    @Nullable public static final BlockType WHITE_WOOL = null;
    @Nullable public static final BlockType WITHER_SKELETON_SKULL = null;
    @Nullable public static final BlockType WITHER_SKELETON_WALL_SKULL = null;
    @Nullable public static final BlockType YELLOW_BANNER = null;
    @Nullable public static final BlockType YELLOW_BED = null;
    @Nullable public static final BlockType YELLOW_CARPET = null;
    @Nullable public static final BlockType YELLOW_CONCRETE = null;
    @Nullable public static final BlockType YELLOW_CONCRETE_POWDER = null;
    @Nullable public static final BlockType YELLOW_GLAZED_TERRACOTTA = null;
    @Nullable public static final BlockType YELLOW_SHULKER_BOX = null;
    @Nullable public static final BlockType YELLOW_STAINED_GLASS = null;
    @Nullable public static final BlockType YELLOW_STAINED_GLASS_PANE = null;
    @Nullable public static final BlockType YELLOW_TERRACOTTA = null;
    @Nullable public static final BlockType YELLOW_WALL_BANNER = null;
    @Nullable public static final BlockType YELLOW_WOOL = null;
    @Nullable public static final BlockType ZOMBIE_HEAD = null;
    @Nullable public static final BlockType ZOMBIE_WALL_HEAD = null;

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
                    bitOffset += property.getNumBits();

                    maxInternalStateId += (property.getValues().size() << bitOffset);
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
                        states.add(new BlockState(type, stateId, ordinal));
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
        int[] result = new int[maxStateId + 1];
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
    public static final int BIT_MASK; // Used internally

    private static final Map<String, BlockType> $REGISTRY = new HashMap<>();

    public static final BlockType[] values;
    public static final BlockState[] states;

    private static final Set<String> $NAMESPACES = new LinkedHashSet<String>();

    static {
        try {
            ArrayList<BlockState> stateList = new ArrayList<>();

            Collection<String> blocks = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().registerBlocks();
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
                    String id = "minecraft:" + field.getName().toLowerCase();
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
                    String id = entry.getKey();
                    String defaultState = entry.getValue();
                    // Skip already registered ids
                    for (; values[internalId] != null; internalId++);
                    BlockType type = register(defaultState, internalId, stateList);
                    values[internalId] = type;
                }
            }

            // Add to $Registry
            for (BlockType type : values) {
                $REGISTRY.put(type.getId().toLowerCase(), type);
            }
            states = stateList.toArray(new BlockState[stateList.size()]);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static BlockType register(final String id, int internalId, List<BlockState> states) {
        // Get the enum name (remove namespace if minecraft:)
        int propStart = id.indexOf('[');
        String typeName = id.substring(0, propStart == -1 ? id.length() : propStart);
        String enumName = (typeName.startsWith("minecraft:") ? typeName.substring(10) : typeName).toUpperCase();
        BlockType existing = new BlockType(id, internalId, states);


        // Set field value
        try {
            Field field = BlockTypes.class.getDeclaredField(enumName);
            ReflectionUtils.setFailsafeFieldValue(field, null, existing);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // register states
        if (typeName.startsWith("minecraft:")) $REGISTRY.put(typeName.substring(10), existing);
        $REGISTRY.put(typeName, existing);
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
        final String inputLower = type.toLowerCase();
        String input = inputLower;

        if (!input.split("\\[", 2)[0].contains(":")) input = "minecraft:" + input;
        BlockType result = $REGISTRY.get(input);
        if (result != null) return result;

        try {
            BlockStateHolder block = LegacyMapper.getInstance().getBlockFromLegacy(input);
            if (block != null) return block.getBlockType();
        } catch (NumberFormatException e) {
        } catch (IndexOutOfBoundsException e) {}

        throw new SuggestInputParseException("Does not match a valid block type: " + inputLower, inputLower, () -> Stream.of(BlockTypes.values)
            .filter(b -> b.getId().contains(inputLower))
            .map(e1 -> e1.getId())
            .collect(Collectors.toList())
        );
    }

    public static Set<String> getNameSpaces() {
        return $NAMESPACES;
    }

    public static final @Nullable BlockType get(final String id) {
        return $REGISTRY.get(id);
    }

    public static final @Nullable BlockType get(final CharSequence id) {
        return $REGISTRY.get(id);
    }

    @Deprecated
    public static final BlockType get(final int ordinal) {
        return values[ordinal];
    }

    @Deprecated
    public static final BlockType getFromStateId(final int internalStateId) {
        return values[internalStateId & BIT_MASK];
    }

    @Deprecated
    public static final BlockType getFromStateOrdinal(final int internalStateOrdinal) {
        return states[internalStateOrdinal].getBlockType();
    }

    public static int size() {
        return values.length;
    }
    
}
