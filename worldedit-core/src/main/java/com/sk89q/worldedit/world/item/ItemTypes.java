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

package com.sk89q.worldedit.world.item;

import com.fastasyncworldedit.core.util.JoinedCharSequence;
import com.fastasyncworldedit.core.world.block.ItemTypesCache;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Locale;

/**
 * Stores a list of common {@link ItemType ItemTypes}.
 *
 * @see ItemType
 */
@SuppressWarnings("unused")
public final class ItemTypes {

    //FAWE start - init
    @Nullable
    public static final ItemType ACACIA_BOAT = init();
    @Nullable
    public static final ItemType ACACIA_BUTTON = init();
    @Nullable
    public static final ItemType ACACIA_DOOR = init();
    @Nullable
    public static final ItemType ACACIA_FENCE = init();
    @Nullable
    public static final ItemType ACACIA_FENCE_GATE = init();
    @Nullable
    public static final ItemType ACACIA_LEAVES = init();
    @Nullable
    public static final ItemType ACACIA_LOG = init();
    @Nullable
    public static final ItemType ACACIA_PLANKS = init();
    @Nullable
    public static final ItemType ACACIA_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType ACACIA_SAPLING = init();
    @Nullable
    public static final ItemType ACACIA_SIGN = init();
    @Nullable
    public static final ItemType ACACIA_SLAB = init();
    @Nullable
    public static final ItemType ACACIA_STAIRS = init();
    @Nullable
    public static final ItemType ACACIA_TRAPDOOR = init();
    @Nullable
    public static final ItemType ACACIA_WOOD = init();
    @Nullable
    public static final ItemType ACTIVATOR_RAIL = init();
    @Nullable
    public static final ItemType AIR = init();
    @Nullable
    public static final ItemType ALLIUM = init();
    @Nullable
    public static final ItemType AMETHYST_BLOCK = init();
    @Nullable
    public static final ItemType AMETHYST_CLUSTER = init();
    @Nullable
    public static final ItemType AMETHYST_SHARD = init();
    @Nullable
    public static final ItemType ANCIENT_DEBRIS = init();
    @Nullable
    public static final ItemType ANDESITE = init();
    @Nullable
    public static final ItemType ANDESITE_SLAB = init();
    @Nullable
    public static final ItemType ANDESITE_STAIRS = init();
    @Nullable
    public static final ItemType ANDESITE_WALL = init();
    @Nullable
    public static final ItemType ANVIL = init();
    @Nullable
    public static final ItemType APPLE = init();
    @Nullable
    public static final ItemType ARMOR_STAND = init();
    @Nullable
    public static final ItemType ARROW = init();
    @Nullable
    public static final ItemType AXOLOTL_BUCKET = init();
    @Nullable
    public static final ItemType AXOLOTL_SPAWN_EGG = init();
    @Nullable
    public static final ItemType AZALEA = init();
    @Nullable
    public static final ItemType AZALEA_LEAVES = init();
    @Nullable
    public static final ItemType AZURE_BLUET = init();
    @Nullable
    public static final ItemType BAKED_POTATO = init();
    @Nullable
    public static final ItemType BAMBOO = init();
    @Nullable
    public static final ItemType BARREL = init();
    @Nullable
    public static final ItemType BARRIER = init();
    @Nullable
    public static final ItemType BASALT = init();
    @Nullable
    public static final ItemType BAT_SPAWN_EGG = init();
    @Nullable
    public static final ItemType BEACON = init();
    @Nullable
    public static final ItemType BEDROCK = init();
    @Nullable
    public static final ItemType BEE_NEST = init();
    @Nullable
    public static final ItemType BEE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType BEEF = init();
    @Nullable
    public static final ItemType BEEHIVE = init();
    @Nullable
    public static final ItemType BEETROOT = init();
    @Nullable
    public static final ItemType BEETROOT_SEEDS = init();
    @Nullable
    public static final ItemType BEETROOT_SOUP = init();
    @Nullable
    public static final ItemType BELL = init();
    @Nullable
    public static final ItemType BIG_DRIPLEAF = init();
    @Nullable
    public static final ItemType BIRCH_BOAT = init();
    @Nullable
    public static final ItemType BIRCH_BUTTON = init();
    @Nullable
    public static final ItemType BIRCH_DOOR = init();
    @Nullable
    public static final ItemType BIRCH_FENCE = init();
    @Nullable
    public static final ItemType BIRCH_FENCE_GATE = init();
    @Nullable
    public static final ItemType BIRCH_LEAVES = init();
    @Nullable
    public static final ItemType BIRCH_LOG = init();
    @Nullable
    public static final ItemType BIRCH_PLANKS = init();
    @Nullable
    public static final ItemType BIRCH_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType BIRCH_SAPLING = init();
    @Nullable
    public static final ItemType BIRCH_SIGN = init();
    @Nullable
    public static final ItemType BIRCH_SLAB = init();
    @Nullable
    public static final ItemType BIRCH_STAIRS = init();
    @Nullable
    public static final ItemType BIRCH_TRAPDOOR = init();
    @Nullable
    public static final ItemType BIRCH_WOOD = init();
    @Nullable
    public static final ItemType BLACK_BANNER = init();
    @Nullable
    public static final ItemType BLACK_BED = init();
    @Nullable
    public static final ItemType BLACK_CANDLE = init();
    @Nullable
    public static final ItemType BLACK_CARPET = init();
    @Nullable
    public static final ItemType BLACK_CONCRETE = init();
    @Nullable
    public static final ItemType BLACK_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType BLACK_DYE = init();
    @Nullable
    public static final ItemType BLACK_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType BLACK_SHULKER_BOX = init();
    @Nullable
    public static final ItemType BLACK_STAINED_GLASS = init();
    @Nullable
    public static final ItemType BLACK_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType BLACK_TERRACOTTA = init();
    @Nullable
    public static final ItemType BLACK_WOOL = init();
    @Nullable
    public static final ItemType BLACKSTONE = init();
    @Nullable
    public static final ItemType BLACKSTONE_SLAB = init();
    @Nullable
    public static final ItemType BLACKSTONE_STAIRS = init();
    @Nullable
    public static final ItemType BLACKSTONE_WALL = init();
    @Nullable
    public static final ItemType BLAST_FURNACE = init();
    @Nullable
    public static final ItemType BLAZE_POWDER = init();
    @Nullable
    public static final ItemType BLAZE_ROD = init();
    @Nullable
    public static final ItemType BLAZE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType BLUE_BANNER = init();
    @Nullable
    public static final ItemType BLUE_BED = init();
    @Nullable
    public static final ItemType BLUE_CANDLE = init();
    @Nullable
    public static final ItemType BROWN_CANDLE = init();
    @Nullable
    public static final ItemType BLUE_CARPET = init();
    @Nullable
    public static final ItemType BLUE_CONCRETE = init();
    @Nullable
    public static final ItemType BLUE_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType BLUE_DYE = init();
    @Nullable
    public static final ItemType BLUE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType BLUE_ICE = init();
    @Nullable
    public static final ItemType BLUE_ORCHID = init();
    @Nullable
    public static final ItemType BLUE_SHULKER_BOX = init();
    @Nullable
    public static final ItemType BLUE_STAINED_GLASS = init();
    @Nullable
    public static final ItemType BLUE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType BLUE_TERRACOTTA = init();
    @Nullable
    public static final ItemType BLUE_WOOL = init();
    @Nullable
    public static final ItemType BONE = init();
    @Nullable
    public static final ItemType BONE_BLOCK = init();
    @Nullable
    public static final ItemType BONE_MEAL = init();
    @Nullable
    public static final ItemType BOOK = init();
    @Nullable
    public static final ItemType BOOKSHELF = init();
    @Nullable
    public static final ItemType BOW = init();
    @Nullable
    public static final ItemType BOWL = init();
    @Nullable
    public static final ItemType BRAIN_CORAL = init();
    @Nullable
    public static final ItemType BRAIN_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType BRAIN_CORAL_FAN = init();
    @Nullable
    public static final ItemType BREAD = init();
    @Nullable
    public static final ItemType BREWING_STAND = init();
    @Nullable
    public static final ItemType BRICK = init();
    @Nullable
    public static final ItemType BRICK_SLAB = init();
    @Nullable
    public static final ItemType BRICK_STAIRS = init();
    @Nullable
    public static final ItemType BRICK_WALL = init();
    @Nullable
    public static final ItemType BRICKS = init();
    @Nullable
    public static final ItemType BROWN_BANNER = init();
    @Nullable
    public static final ItemType BROWN_BED = init();
    @Nullable
    public static final ItemType BROWN_CARPET = init();
    @Nullable
    public static final ItemType BROWN_CONCRETE = init();
    @Nullable
    public static final ItemType BROWN_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType BROWN_DYE = init();
    @Nullable
    public static final ItemType BROWN_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType BROWN_MUSHROOM = init();
    @Nullable
    public static final ItemType BROWN_MUSHROOM_BLOCK = init();
    @Nullable
    public static final ItemType BROWN_SHULKER_BOX = init();
    @Nullable
    public static final ItemType BROWN_STAINED_GLASS = init();
    @Nullable
    public static final ItemType BROWN_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType BROWN_TERRACOTTA = init();
    @Nullable
    public static final ItemType BROWN_WOOL = init();
    @Nullable
    public static final ItemType BUBBLE_CORAL = init();
    @Nullable
    public static final ItemType BUBBLE_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType BUBBLE_CORAL_FAN = init();
    @Nullable
    public static final ItemType BUCKET = init();
    @Nullable
    public static final ItemType BUDDING_AMETHYST = init();
    @Nullable
    public static final ItemType BUNDLE = init();
    @Nullable
    public static final ItemType CACTUS = init();
    @Deprecated
    @Nullable
    public static final ItemType CACTUS_GREEN = init();
    @Nullable
    public static final ItemType CAKE = init();
    @Nullable
    public static final ItemType CALCITE = init();
    @Nullable
    public static final ItemType CAMPFIRE = init();
    @Nullable
    public static final ItemType CANDLE = init();
    @Nullable
    public static final ItemType CARROT = init();
    @Nullable
    public static final ItemType CARROT_ON_A_STICK = init();
    @Nullable
    public static final ItemType CARTOGRAPHY_TABLE = init();
    @Nullable
    public static final ItemType CARVED_PUMPKIN = init();
    @Nullable
    public static final ItemType CAT_SPAWN_EGG = init();
    @Nullable
    public static final ItemType CAULDRON = init();
    @Nullable
    public static final ItemType CAVE_SPIDER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType CHAIN = init();
    @Nullable
    public static final ItemType CHAIN_COMMAND_BLOCK = init();
    @Nullable
    public static final ItemType CHAINMAIL_BOOTS = init();
    @Nullable
    public static final ItemType CHAINMAIL_CHESTPLATE = init();
    @Nullable
    public static final ItemType CHAINMAIL_HELMET = init();
    @Nullable
    public static final ItemType CHAINMAIL_LEGGINGS = init();
    @Nullable
    public static final ItemType CHARCOAL = init();
    @Nullable
    public static final ItemType CHEST = init();
    @Nullable
    public static final ItemType CHEST_MINECART = init();
    @Nullable
    public static final ItemType CHICKEN = init();
    @Nullable
    public static final ItemType CHICKEN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType CHIPPED_ANVIL = init();
    @Nullable
    public static final ItemType CHISELED_DEEPSLATE = init();
    @Nullable
    public static final ItemType CHISELED_NETHER_BRICKS = init();
    @Nullable
    public static final ItemType CHISELED_POLISHED_BLACKSTONE = init();
    @Nullable
    public static final ItemType CHISELED_QUARTZ_BLOCK = init();
    @Nullable
    public static final ItemType CHISELED_RED_SANDSTONE = init();
    @Nullable
    public static final ItemType CHISELED_SANDSTONE = init();
    @Nullable
    public static final ItemType CHISELED_STONE_BRICKS = init();
    @Nullable
    public static final ItemType CHORUS_FLOWER = init();
    @Nullable
    public static final ItemType CHORUS_FRUIT = init();
    @Nullable
    public static final ItemType CHORUS_PLANT = init();
    @Nullable
    public static final ItemType CLAY = init();
    @Nullable
    public static final ItemType CLAY_BALL = init();
    @Nullable
    public static final ItemType CLOCK = init();
    @Nullable
    public static final ItemType COAL = init();
    @Nullable
    public static final ItemType COAL_BLOCK = init();
    @Nullable
    public static final ItemType COAL_ORE = init();
    @Nullable
    public static final ItemType COARSE_DIRT = init();
    @Nullable
    public static final ItemType COBBLED_DEEPSLATE = init();
    @Nullable
    public static final ItemType COBBLED_DEEPSLATE_SLAB = init();
    @Nullable
    public static final ItemType COBBLED_DEEPSLATE_STAIRS = init();
    @Nullable
    public static final ItemType COBBLED_DEEPSLATE_WALL = init();
    @Nullable
    public static final ItemType COBBLESTONE = init();
    @Nullable
    public static final ItemType COBBLESTONE_SLAB = init();
    @Nullable
    public static final ItemType COBBLESTONE_STAIRS = init();
    @Nullable
    public static final ItemType COBBLESTONE_WALL = init();
    @Nullable
    public static final ItemType COBWEB = init();
    @Nullable
    public static final ItemType COCOA_BEANS = init();
    @Nullable
    public static final ItemType COD = init();
    @Nullable
    public static final ItemType COD_BUCKET = init();
    @Nullable
    public static final ItemType COD_SPAWN_EGG = init();
    @Nullable
    public static final ItemType COMMAND_BLOCK = init();
    @Nullable
    public static final ItemType COMMAND_BLOCK_MINECART = init();
    @Nullable
    public static final ItemType COMPARATOR = init();
    @Nullable
    public static final ItemType COMPASS = init();
    @Nullable
    public static final ItemType COMPOSTER = init();
    @Nullable
    public static final ItemType CONDUIT = init();
    @Nullable
    public static final ItemType COOKED_BEEF = init();
    @Nullable
    public static final ItemType COOKED_CHICKEN = init();
    @Nullable
    public static final ItemType COOKED_COD = init();
    @Nullable
    public static final ItemType COOKED_MUTTON = init();
    @Nullable
    public static final ItemType COOKED_PORKCHOP = init();
    @Nullable
    public static final ItemType COOKED_RABBIT = init();
    @Nullable
    public static final ItemType COOKED_SALMON = init();
    @Nullable
    public static final ItemType COOKIE = init();
    @Nullable
    public static final ItemType COPPER_BLOCK = init();
    @Nullable
    public static final ItemType COPPER_INGOT = init();
    @Nullable
    public static final ItemType COPPER_ORE = init();
    @Nullable
    public static final ItemType CORNFLOWER = init();
    @Nullable
    public static final ItemType COW_SPAWN_EGG = init();
    @Nullable
    public static final ItemType CRACKED_DEEPSLATE_BRICKS = init();
    @Nullable
    public static final ItemType CRACKED_DEEPSLATE_TILES = init();
    @Nullable
    public static final ItemType CRACKED_NETHER_BRICKS = init();
    @Nullable
    public static final ItemType CRACKED_POLISHED_BLACKSTONE_BRICKS = init();
    @Nullable
    public static final ItemType CRACKED_STONE_BRICKS = init();
    @Nullable
    public static final ItemType CRAFTING_TABLE = init();
    @Nullable
    public static final ItemType CREEPER_BANNER_PATTERN = init();
    @Nullable
    public static final ItemType CREEPER_HEAD = init();
    @Nullable
    public static final ItemType CREEPER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType CRIMSON_BUTTON = init();
    @Nullable
    public static final ItemType CRIMSON_DOOR = init();
    @Nullable
    public static final ItemType CRIMSON_FENCE = init();
    @Nullable
    public static final ItemType CRIMSON_FENCE_GATE = init();
    @Nullable
    public static final ItemType CRIMSON_FUNGUS = init();
    @Nullable
    public static final ItemType CRIMSON_HYPHAE = init();
    @Nullable
    public static final ItemType CRIMSON_NYLIUM = init();
    @Nullable
    public static final ItemType CRIMSON_PLANKS = init();
    @Nullable
    public static final ItemType CRIMSON_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType CRIMSON_ROOTS = init();
    @Nullable
    public static final ItemType CRIMSON_SIGN = init();
    @Nullable
    public static final ItemType CRIMSON_SLAB = init();
    @Nullable
    public static final ItemType CRIMSON_STAIRS = init();
    @Nullable
    public static final ItemType CRIMSON_STEM = init();
    @Nullable
    public static final ItemType CRIMSON_TRAPDOOR = init();
    @Nullable
    public static final ItemType CROSSBOW = init();
    @Nullable
    public static final ItemType CRYING_OBSIDIAN = init();
    @Nullable
    public static final ItemType CUT_COPPER = init();
    @Nullable
    public static final ItemType CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType CUT_RED_SANDSTONE = init();
    @Nullable
    public static final ItemType CUT_RED_SANDSTONE_SLAB = init();
    @Nullable
    public static final ItemType CUT_SANDSTONE = init();
    @Nullable
    public static final ItemType CUT_SANDSTONE_SLAB = init();
    @Nullable
    public static final ItemType CYAN_BANNER = init();
    @Nullable
    public static final ItemType CYAN_BED = init();
    @Nullable
    public static final ItemType CYAN_CANDLE = init();
    @Nullable
    public static final ItemType CYAN_CARPET = init();
    @Nullable
    public static final ItemType CYAN_CONCRETE = init();
    @Nullable
    public static final ItemType CYAN_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType CYAN_DYE = init();
    @Nullable
    public static final ItemType CYAN_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType CYAN_SHULKER_BOX = init();
    @Nullable
    public static final ItemType CYAN_STAINED_GLASS = init();
    @Nullable
    public static final ItemType CYAN_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType CYAN_TERRACOTTA = init();
    @Nullable
    public static final ItemType CYAN_WOOL = init();
    @Nullable
    public static final ItemType DAMAGED_ANVIL = init();
    @Nullable
    public static final ItemType DANDELION = init();
    @Deprecated
    @Nullable
    public static final ItemType DANDELION_YELLOW = init();
    @Nullable
    public static final ItemType DARK_OAK_BOAT = init();
    @Nullable
    public static final ItemType DARK_OAK_BUTTON = init();
    @Nullable
    public static final ItemType DARK_OAK_DOOR = init();
    @Nullable
    public static final ItemType DARK_OAK_FENCE = init();
    @Nullable
    public static final ItemType DARK_OAK_FENCE_GATE = init();
    @Nullable
    public static final ItemType DARK_OAK_LEAVES = init();
    @Nullable
    public static final ItemType DARK_OAK_LOG = init();
    @Nullable
    public static final ItemType DARK_OAK_PLANKS = init();
    @Nullable
    public static final ItemType DARK_OAK_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType DARK_OAK_SAPLING = init();
    @Nullable
    public static final ItemType DARK_OAK_SIGN = init();
    @Nullable
    public static final ItemType DARK_OAK_SLAB = init();
    @Nullable
    public static final ItemType DARK_OAK_STAIRS = init();
    @Nullable
    public static final ItemType DARK_OAK_TRAPDOOR = init();
    @Nullable
    public static final ItemType DARK_OAK_WOOD = init();
    @Nullable
    public static final ItemType DARK_PRISMARINE = init();
    @Nullable
    public static final ItemType DARK_PRISMARINE_SLAB = init();
    @Nullable
    public static final ItemType DARK_PRISMARINE_STAIRS = init();
    @Nullable
    public static final ItemType DAYLIGHT_DETECTOR = init();
    @Nullable
    public static final ItemType DEAD_BRAIN_CORAL = init();
    @Nullable
    public static final ItemType DEAD_BRAIN_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType DEAD_BRAIN_CORAL_FAN = init();
    @Nullable
    public static final ItemType DEAD_BUBBLE_CORAL = init();
    @Nullable
    public static final ItemType DEAD_BUBBLE_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType DEAD_BUBBLE_CORAL_FAN = init();
    @Nullable
    public static final ItemType DEAD_BUSH = init();
    @Nullable
    public static final ItemType DEAD_FIRE_CORAL = init();
    @Nullable
    public static final ItemType DEAD_FIRE_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType DEAD_FIRE_CORAL_FAN = init();
    @Nullable
    public static final ItemType DEAD_HORN_CORAL = init();
    @Nullable
    public static final ItemType DEAD_HORN_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType DEAD_HORN_CORAL_FAN = init();
    @Nullable
    public static final ItemType DEAD_TUBE_CORAL = init();
    @Nullable
    public static final ItemType DEAD_TUBE_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType DEAD_TUBE_CORAL_FAN = init();
    @Nullable
    public static final ItemType DEBUG_STICK = init();
    @Nullable
    public static final ItemType DEEPSLATE = init();
    @Nullable
    public static final ItemType DEEPSLATE_BRICK_SLAB = init();
    @Nullable
    public static final ItemType DEEPSLATE_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType DEEPSLATE_BRICK_WALL = init();
    @Nullable
    public static final ItemType DEEPSLATE_BRICKS = init();
    @Nullable
    public static final ItemType DEEPSLATE_COAL_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_COPPER_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_DIAMOND_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_EMERALD_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_GOLD_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_IRON_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_LAPIS_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_REDSTONE_ORE = init();
    @Nullable
    public static final ItemType DEEPSLATE_TILE_SLAB = init();
    @Nullable
    public static final ItemType DEEPSLATE_TILE_STAIRS = init();
    @Nullable
    public static final ItemType DEEPSLATE_TILE_WALL = init();
    @Nullable
    public static final ItemType DEEPSLATE_TILES = init();
    @Nullable
    public static final ItemType DETECTOR_RAIL = init();
    @Nullable
    public static final ItemType DIAMOND = init();
    @Nullable
    public static final ItemType DIAMOND_AXE = init();
    @Nullable
    public static final ItemType DIAMOND_BLOCK = init();
    @Nullable
    public static final ItemType DIAMOND_BOOTS = init();
    @Nullable
    public static final ItemType DIAMOND_CHESTPLATE = init();
    @Nullable
    public static final ItemType DIAMOND_HELMET = init();
    @Nullable
    public static final ItemType DIAMOND_HOE = init();
    @Nullable
    public static final ItemType DIAMOND_HORSE_ARMOR = init();
    @Nullable
    public static final ItemType DIAMOND_LEGGINGS = init();
    @Nullable
    public static final ItemType DIAMOND_ORE = init();
    @Nullable
    public static final ItemType DIAMOND_PICKAXE = init();
    @Nullable
    public static final ItemType DIAMOND_SHOVEL = init();
    @Nullable
    public static final ItemType DIAMOND_SWORD = init();
    @Nullable
    public static final ItemType DIORITE = init();
    @Nullable
    public static final ItemType DIORITE_SLAB = init();
    @Nullable
    public static final ItemType DIORITE_STAIRS = init();
    @Nullable
    public static final ItemType DIORITE_WALL = init();
    @Nullable
    public static final ItemType DIRT = init();
    @Nullable
    public static final ItemType DIRT_PATH = init();
    @Nullable
    public static final ItemType DISPENSER = init();
    @Nullable
    public static final ItemType DOLPHIN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType DONKEY_SPAWN_EGG = init();
    @Nullable
    public static final ItemType DRAGON_BREATH = init();
    @Nullable
    public static final ItemType DRAGON_EGG = init();
    @Nullable
    public static final ItemType DRAGON_HEAD = init();
    @Nullable
    public static final ItemType DRIED_KELP = init();
    @Nullable
    public static final ItemType DRIED_KELP_BLOCK = init();
    @Nullable
    public static final ItemType DRIPSTONE_BLOCK = init();
    @Nullable
    public static final ItemType DROPPER = init();
    @Nullable
    public static final ItemType DROWNED_SPAWN_EGG = init();
    @Nullable
    public static final ItemType EGG = init();
    @Nullable
    public static final ItemType ELDER_GUARDIAN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ELYTRA = init();
    @Nullable
    public static final ItemType EMERALD = init();
    @Nullable
    public static final ItemType EMERALD_BLOCK = init();
    @Nullable
    public static final ItemType EMERALD_ORE = init();
    @Nullable
    public static final ItemType ENCHANTED_BOOK = init();
    @Nullable
    public static final ItemType ENCHANTED_GOLDEN_APPLE = init();
    @Nullable
    public static final ItemType ENCHANTING_TABLE = init();
    @Nullable
    public static final ItemType END_CRYSTAL = init();
    @Nullable
    public static final ItemType END_PORTAL_FRAME = init();
    @Nullable
    public static final ItemType END_ROD = init();
    @Nullable
    public static final ItemType END_STONE = init();
    @Nullable
    public static final ItemType END_STONE_BRICK_SLAB = init();
    @Nullable
    public static final ItemType END_STONE_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType END_STONE_BRICK_WALL = init();
    @Nullable
    public static final ItemType END_STONE_BRICKS = init();
    @Nullable
    public static final ItemType ENDER_CHEST = init();
    @Nullable
    public static final ItemType ENDER_EYE = init();
    @Nullable
    public static final ItemType ENDER_PEARL = init();
    @Nullable
    public static final ItemType ENDERMAN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ENDERMITE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType EVOKER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType EXPERIENCE_BOTTLE = init();
    @Nullable
    public static final ItemType EXPOSED_COPPER = init();
    @Nullable
    public static final ItemType EXPOSED_CUT_COPPER = init();
    @Nullable
    public static final ItemType EXPOSED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType EXPOSED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType FARMLAND = init();
    @Nullable
    public static final ItemType FEATHER = init();
    @Nullable
    public static final ItemType FERMENTED_SPIDER_EYE = init();
    @Nullable
    public static final ItemType FERN = init();
    @Nullable
    public static final ItemType FILLED_MAP = init();
    @Nullable
    public static final ItemType FIRE_CHARGE = init();
    @Nullable
    public static final ItemType FIRE_CORAL = init();
    @Nullable
    public static final ItemType FIRE_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType FIRE_CORAL_FAN = init();
    @Nullable
    public static final ItemType FIREWORK_ROCKET = init();
    @Nullable
    public static final ItemType FIREWORK_STAR = init();
    @Nullable
    public static final ItemType FISHING_ROD = init();
    @Nullable
    public static final ItemType FLETCHING_TABLE = init();
    @Nullable
    public static final ItemType FLINT = init();
    @Nullable
    public static final ItemType FLINT_AND_STEEL = init();
    @Nullable
    public static final ItemType FLOWER_BANNER_PATTERN = init();
    @Nullable
    public static final ItemType FLOWER_POT = init();
    @Nullable
    public static final ItemType FLOWERING_AZALEA = init();
    @Nullable
    public static final ItemType FLOWERING_AZALEA_LEAVES = init();
    @Nullable
    public static final ItemType FOX_SPAWN_EGG = init();
    @Nullable
    public static final ItemType FURNACE = init();
    @Nullable
    public static final ItemType FURNACE_MINECART = init();
    @Nullable
    public static final ItemType GHAST_SPAWN_EGG = init();
    @Nullable
    public static final ItemType GHAST_TEAR = init();
    @Nullable
    public static final ItemType GILDED_BLACKSTONE = init();
    @Nullable
    public static final ItemType GLASS = init();
    @Nullable
    public static final ItemType GLASS_BOTTLE = init();
    @Nullable
    public static final ItemType GLASS_PANE = init();
    @Nullable
    public static final ItemType GLISTERING_MELON_SLICE = init();
    @Nullable
    public static final ItemType GLOBE_BANNER_PATTERN = init();
    @Nullable
    public static final ItemType GLOW_BERRIES = init();
    @Nullable
    public static final ItemType GLOW_INK_SAC = init();
    @Nullable
    public static final ItemType GLOW_ITEM_FRAME = init();
    @Nullable
    public static final ItemType GLOW_LICHEN = init();
    @Nullable
    public static final ItemType GLOW_SQUID_SPAWN_EGG = init();
    @Nullable
    public static final ItemType GLOWSTONE = init();
    @Nullable
    public static final ItemType GLOWSTONE_DUST = init();
    @Nullable
    public static final ItemType GOAT_SPAWN_EGG = init();
    @Nullable
    public static final ItemType GOLD_BLOCK = init();
    @Nullable
    public static final ItemType GOLD_INGOT = init();
    @Nullable
    public static final ItemType GOLD_NUGGET = init();
    @Nullable
    public static final ItemType GOLD_ORE = init();
    @Nullable
    public static final ItemType GOLDEN_APPLE = init();
    @Nullable
    public static final ItemType GOLDEN_AXE = init();
    @Nullable
    public static final ItemType GOLDEN_BOOTS = init();
    @Nullable
    public static final ItemType GOLDEN_CARROT = init();
    @Nullable
    public static final ItemType GOLDEN_CHESTPLATE = init();
    @Nullable
    public static final ItemType GOLDEN_HELMET = init();
    @Nullable
    public static final ItemType GOLDEN_HOE = init();
    @Nullable
    public static final ItemType GOLDEN_HORSE_ARMOR = init();
    @Nullable
    public static final ItemType GOLDEN_LEGGINGS = init();
    @Nullable
    public static final ItemType GOLDEN_PICKAXE = init();
    @Nullable
    public static final ItemType GOLDEN_SHOVEL = init();
    @Nullable
    public static final ItemType GOLDEN_SWORD = init();
    @Nullable
    public static final ItemType GRANITE = init();
    @Nullable
    public static final ItemType GRANITE_SLAB = init();
    @Nullable
    public static final ItemType GRANITE_STAIRS = init();
    @Nullable
    public static final ItemType GRANITE_WALL = init();
    @Nullable
    public static final ItemType GRASS = init();
    @Nullable
    public static final ItemType GRASS_BLOCK = init();
    @Deprecated
    @Nullable
    public static final ItemType GRASS_PATH = init();
    @Nullable
    public static final ItemType GRAVEL = init();
    @Nullable
    public static final ItemType GRAY_BANNER = init();
    @Nullable
    public static final ItemType GRAY_BED = init();
    @Nullable
    public static final ItemType GRAY_CANDLE = init();
    @Nullable
    public static final ItemType GRAY_CARPET = init();
    @Nullable
    public static final ItemType GRAY_CONCRETE = init();
    @Nullable
    public static final ItemType GRAY_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType GRAY_DYE = init();
    @Nullable
    public static final ItemType GRAY_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType GRAY_SHULKER_BOX = init();
    @Nullable
    public static final ItemType GRAY_STAINED_GLASS = init();
    @Nullable
    public static final ItemType GRAY_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType GRAY_TERRACOTTA = init();
    @Nullable
    public static final ItemType GRAY_WOOL = init();
    @Nullable
    public static final ItemType GREEN_BANNER = init();
    @Nullable
    public static final ItemType GREEN_BED = init();
    @Nullable
    public static final ItemType GREEN_CANDLE = init();
    @Nullable
    public static final ItemType GREEN_CARPET = init();
    @Nullable
    public static final ItemType GREEN_CONCRETE = init();
    @Nullable
    public static final ItemType GREEN_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType GREEN_DYE = init();
    @Nullable
    public static final ItemType GREEN_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType GREEN_SHULKER_BOX = init();
    @Nullable
    public static final ItemType GREEN_STAINED_GLASS = init();
    @Nullable
    public static final ItemType GREEN_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType GREEN_TERRACOTTA = init();
    @Nullable
    public static final ItemType GREEN_WOOL = init();
    @Nullable
    public static final ItemType GRINDSTONE = init();
    @Nullable
    public static final ItemType GUARDIAN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType GUNPOWDER = init();
    @Nullable
    public static final ItemType HANGING_ROOTS = init();
    @Nullable
    public static final ItemType HAY_BLOCK = init();
    @Nullable
    public static final ItemType HEART_OF_THE_SEA = init();
    @Nullable
    public static final ItemType HEAVY_WEIGHTED_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType HOGLIN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType HONEY_BLOCK = init();
    @Nullable
    public static final ItemType HONEY_BOTTLE = init();
    @Nullable
    public static final ItemType HONEYCOMB = init();
    @Nullable
    public static final ItemType HONEYCOMB_BLOCK = init();
    @Nullable
    public static final ItemType HOPPER = init();
    @Nullable
    public static final ItemType HOPPER_MINECART = init();
    @Nullable
    public static final ItemType HORN_CORAL = init();
    @Nullable
    public static final ItemType HORN_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType HORN_CORAL_FAN = init();
    @Nullable
    public static final ItemType HORSE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType HUSK_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ICE = init();
    @Nullable
    public static final ItemType INFESTED_CHISELED_STONE_BRICKS = init();
    @Nullable
    public static final ItemType INFESTED_COBBLESTONE = init();
    @Nullable
    public static final ItemType INFESTED_CRACKED_STONE_BRICKS = init();
    @Nullable
    public static final ItemType INFESTED_DEEPSLATE = init();
    @Nullable
    public static final ItemType INFESTED_MOSSY_STONE_BRICKS = init();
    @Nullable
    public static final ItemType INFESTED_STONE = init();
    @Nullable
    public static final ItemType INFESTED_STONE_BRICKS = init();
    @Nullable
    public static final ItemType INK_SAC = init();
    @Nullable
    public static final ItemType IRON_AXE = init();
    @Nullable
    public static final ItemType IRON_BARS = init();
    @Nullable
    public static final ItemType IRON_BLOCK = init();
    @Nullable
    public static final ItemType IRON_BOOTS = init();
    @Nullable
    public static final ItemType IRON_CHESTPLATE = init();
    @Nullable
    public static final ItemType IRON_DOOR = init();
    @Nullable
    public static final ItemType IRON_HELMET = init();
    @Nullable
    public static final ItemType IRON_HOE = init();
    @Nullable
    public static final ItemType IRON_HORSE_ARMOR = init();
    @Nullable
    public static final ItemType IRON_INGOT = init();
    @Nullable
    public static final ItemType IRON_LEGGINGS = init();
    @Nullable
    public static final ItemType IRON_NUGGET = init();
    @Nullable
    public static final ItemType IRON_ORE = init();
    @Nullable
    public static final ItemType IRON_PICKAXE = init();
    @Nullable
    public static final ItemType IRON_SHOVEL = init();
    @Nullable
    public static final ItemType IRON_SWORD = init();
    @Nullable
    public static final ItemType IRON_TRAPDOOR = init();
    @Nullable
    public static final ItemType ITEM_FRAME = init();
    @Nullable
    public static final ItemType JACK_O_LANTERN = init();
    @Nullable
    public static final ItemType JIGSAW = init();
    @Nullable
    public static final ItemType JUKEBOX = init();
    @Nullable
    public static final ItemType JUNGLE_BOAT = init();
    @Nullable
    public static final ItemType JUNGLE_BUTTON = init();
    @Nullable
    public static final ItemType JUNGLE_DOOR = init();
    @Nullable
    public static final ItemType JUNGLE_FENCE = init();
    @Nullable
    public static final ItemType JUNGLE_FENCE_GATE = init();
    @Nullable
    public static final ItemType JUNGLE_LEAVES = init();
    @Nullable
    public static final ItemType JUNGLE_LOG = init();
    @Nullable
    public static final ItemType JUNGLE_PLANKS = init();
    @Nullable
    public static final ItemType JUNGLE_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType JUNGLE_SAPLING = init();
    @Nullable
    public static final ItemType JUNGLE_SIGN = init();
    @Nullable
    public static final ItemType JUNGLE_SLAB = init();
    @Nullable
    public static final ItemType JUNGLE_STAIRS = init();
    @Nullable
    public static final ItemType JUNGLE_TRAPDOOR = init();
    @Nullable
    public static final ItemType JUNGLE_WOOD = init();
    @Nullable
    public static final ItemType KELP = init();
    @Nullable
    public static final ItemType KNOWLEDGE_BOOK = init();
    @Nullable
    public static final ItemType LADDER = init();
    @Nullable
    public static final ItemType LANTERN = init();
    @Nullable
    public static final ItemType LAPIS_BLOCK = init();
    @Nullable
    public static final ItemType LAPIS_LAZULI = init();
    @Nullable
    public static final ItemType LAPIS_ORE = init();
    @Nullable
    public static final ItemType LARGE_AMETHYST_BUD = init();
    @Nullable
    public static final ItemType LARGE_FERN = init();
    @Nullable
    public static final ItemType LAVA_BUCKET = init();
    @Nullable
    public static final ItemType LEAD = init();
    @Nullable
    public static final ItemType LEATHER = init();
    @Nullable
    public static final ItemType LEATHER_BOOTS = init();
    @Nullable
    public static final ItemType LEATHER_CHESTPLATE = init();
    @Nullable
    public static final ItemType LEATHER_HELMET = init();
    @Nullable
    public static final ItemType LEATHER_HORSE_ARMOR = init();
    @Nullable
    public static final ItemType LEATHER_LEGGINGS = init();
    @Nullable
    public static final ItemType LECTERN = init();
    @Nullable
    public static final ItemType LEVER = init();
    @Nullable
    public static final ItemType LIGHT = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_BANNER = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_BED = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_CANDLE = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_CARPET = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_CONCRETE = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_DYE = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_SHULKER_BOX = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_STAINED_GLASS = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_TERRACOTTA = init();
    @Nullable
    public static final ItemType LIGHT_BLUE_WOOL = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_BANNER = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_BED = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_CANDLE = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_CARPET = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_CONCRETE = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_DYE = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_SHULKER_BOX = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_STAINED_GLASS = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_TERRACOTTA = init();
    @Nullable
    public static final ItemType LIGHT_GRAY_WOOL = init();
    @Nullable
    public static final ItemType LIGHT_WEIGHTED_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType LIGHTNING_ROD = init();
    @Nullable
    public static final ItemType LILAC = init();
    @Nullable
    public static final ItemType LILY_OF_THE_VALLEY = init();
    @Nullable
    public static final ItemType LILY_PAD = init();
    @Nullable
    public static final ItemType LIME_BANNER = init();
    @Nullable
    public static final ItemType LIME_BED = init();
    @Nullable
    public static final ItemType LIME_CANDLE = init();
    @Nullable
    public static final ItemType LIME_CARPET = init();
    @Nullable
    public static final ItemType LIME_CONCRETE = init();
    @Nullable
    public static final ItemType LIME_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType LIME_DYE = init();
    @Nullable
    public static final ItemType LIME_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType LIME_SHULKER_BOX = init();
    @Nullable
    public static final ItemType LIME_STAINED_GLASS = init();
    @Nullable
    public static final ItemType LIME_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType LIME_TERRACOTTA = init();
    @Nullable
    public static final ItemType LIME_WOOL = init();
    @Nullable
    public static final ItemType LINGERING_POTION = init();
    @Nullable
    public static final ItemType LLAMA_SPAWN_EGG = init();
    @Nullable
    public static final ItemType LODESTONE = init();
    @Nullable
    public static final ItemType LOOM = init();
    @Nullable
    public static final ItemType MAGENTA_BANNER = init();
    @Nullable
    public static final ItemType MAGENTA_BED = init();
    @Nullable
    public static final ItemType MAGENTA_CANDLE = init();
    @Nullable
    public static final ItemType MAGENTA_CARPET = init();
    @Nullable
    public static final ItemType MAGENTA_CONCRETE = init();
    @Nullable
    public static final ItemType MAGENTA_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType MAGENTA_DYE = init();
    @Nullable
    public static final ItemType MAGENTA_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType MAGENTA_SHULKER_BOX = init();
    @Nullable
    public static final ItemType MAGENTA_STAINED_GLASS = init();
    @Nullable
    public static final ItemType MAGENTA_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType MAGENTA_TERRACOTTA = init();
    @Nullable
    public static final ItemType MAGENTA_WOOL = init();
    @Nullable
    public static final ItemType MAGMA_BLOCK = init();
    @Nullable
    public static final ItemType MAGMA_CREAM = init();
    @Nullable
    public static final ItemType MAGMA_CUBE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType MAP = init();
    @Nullable
    public static final ItemType MEDIUM_AMETHYST_BUD = init();
    @Nullable
    public static final ItemType MELON = init();
    @Nullable
    public static final ItemType MELON_SEEDS = init();
    @Nullable
    public static final ItemType MELON_SLICE = init();
    @Nullable
    public static final ItemType MILK_BUCKET = init();
    @Nullable
    public static final ItemType MINECART = init();
    @Nullable
    public static final ItemType MOJANG_BANNER_PATTERN = init();
    @Nullable
    public static final ItemType MOOSHROOM_SPAWN_EGG = init();
    @Nullable
    public static final ItemType MOSS_BLOCK = init();
    @Nullable
    public static final ItemType MOSS_CARPET = init();
    @Nullable
    public static final ItemType MOSSY_COBBLESTONE = init();
    @Nullable
    public static final ItemType MOSSY_COBBLESTONE_SLAB = init();
    @Nullable
    public static final ItemType MOSSY_COBBLESTONE_STAIRS = init();
    @Nullable
    public static final ItemType MOSSY_COBBLESTONE_WALL = init();
    @Nullable
    public static final ItemType MOSSY_STONE_BRICK_SLAB = init();
    @Nullable
    public static final ItemType MOSSY_STONE_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType MOSSY_STONE_BRICK_WALL = init();
    @Nullable
    public static final ItemType MOSSY_STONE_BRICKS = init();
    @Nullable
    public static final ItemType MULE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType MUSHROOM_STEM = init();
    @Nullable
    public static final ItemType MUSHROOM_STEW = init();
    @Nullable
    public static final ItemType MUSIC_DISC_11 = init();
    @Nullable
    public static final ItemType MUSIC_DISC_13 = init();
    @Nullable
    public static final ItemType MUSIC_DISC_BLOCKS = init();
    @Nullable
    public static final ItemType MUSIC_DISC_CAT = init();
    @Nullable
    public static final ItemType MUSIC_DISC_CHIRP = init();
    @Nullable
    public static final ItemType MUSIC_DISC_FAR = init();
    @Nullable
    public static final ItemType MUSIC_DISC_MALL = init();
    @Nullable
    public static final ItemType MUSIC_DISC_MELLOHI = init();
    @Nullable
    public static final ItemType MUSIC_DISC_PIGSTEP = init();
    @Nullable
    public static final ItemType MUSIC_DISC_STAL = init();
    @Nullable
    public static final ItemType MUSIC_DISC_STRAD = init();
    @Nullable
    public static final ItemType MUSIC_DISC_WAIT = init();
    @Nullable
    public static final ItemType MUSIC_DISC_WARD = init();
    @Nullable
    public static final ItemType MUTTON = init();
    @Nullable
    public static final ItemType MYCELIUM = init();
    @Nullable
    public static final ItemType NAME_TAG = init();
    @Nullable
    public static final ItemType NAUTILUS_SHELL = init();
    @Nullable
    public static final ItemType NETHER_BRICK = init();
    @Nullable
    public static final ItemType NETHER_BRICK_FENCE = init();
    @Nullable
    public static final ItemType NETHER_BRICK_SLAB = init();
    @Nullable
    public static final ItemType NETHER_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType NETHER_BRICK_WALL = init();
    @Nullable
    public static final ItemType NETHER_BRICKS = init();
    @Nullable
    public static final ItemType NETHER_GOLD_ORE = init();
    @Nullable
    public static final ItemType NETHER_QUARTZ_ORE = init();
    @Nullable
    public static final ItemType NETHER_SPROUTS = init();
    @Nullable
    public static final ItemType NETHER_STAR = init();
    @Nullable
    public static final ItemType NETHER_WART = init();
    @Nullable
    public static final ItemType NETHER_WART_BLOCK = init();
    @Nullable
    public static final ItemType NETHERITE_AXE = init();
    @Nullable
    public static final ItemType NETHERITE_BLOCK = init();
    @Nullable
    public static final ItemType NETHERITE_BOOTS = init();
    @Nullable
    public static final ItemType NETHERITE_CHESTPLATE = init();
    @Nullable
    public static final ItemType NETHERITE_HELMET = init();
    @Nullable
    public static final ItemType NETHERITE_HOE = init();
    @Nullable
    public static final ItemType NETHERITE_INGOT = init();
    @Nullable
    public static final ItemType NETHERITE_LEGGINGS = init();
    @Nullable
    public static final ItemType NETHERITE_PICKAXE = init();
    @Nullable
    public static final ItemType NETHERITE_SCRAP = init();
    @Nullable
    public static final ItemType NETHERITE_SHOVEL = init();
    @Nullable
    public static final ItemType NETHERITE_SWORD = init();
    @Nullable
    public static final ItemType NETHERRACK = init();
    @Nullable
    public static final ItemType NOTE_BLOCK = init();
    @Nullable
    public static final ItemType OAK_BOAT = init();
    @Nullable
    public static final ItemType OAK_BUTTON = init();
    @Nullable
    public static final ItemType OAK_DOOR = init();
    @Nullable
    public static final ItemType OAK_FENCE = init();
    @Nullable
    public static final ItemType OAK_FENCE_GATE = init();
    @Nullable
    public static final ItemType OAK_LEAVES = init();
    @Nullable
    public static final ItemType OAK_LOG = init();
    @Nullable
    public static final ItemType OAK_PLANKS = init();
    @Nullable
    public static final ItemType OAK_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType OAK_SAPLING = init();
    @Nullable
    public static final ItemType OAK_SIGN = init();
    @Nullable
    public static final ItemType OAK_SLAB = init();
    @Nullable
    public static final ItemType OAK_STAIRS = init();
    @Nullable
    public static final ItemType OAK_TRAPDOOR = init();
    @Nullable
    public static final ItemType OAK_WOOD = init();
    @Nullable
    public static final ItemType OBSERVER = init();
    @Nullable
    public static final ItemType OBSIDIAN = init();
    @Nullable
    public static final ItemType OCELOT_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ORANGE_BANNER = init();
    @Nullable
    public static final ItemType ORANGE_BED = init();
    @Nullable
    public static final ItemType ORANGE_CANDLE = init();
    @Nullable
    public static final ItemType ORANGE_CARPET = init();
    @Nullable
    public static final ItemType ORANGE_CONCRETE = init();
    @Nullable
    public static final ItemType ORANGE_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType ORANGE_DYE = init();
    @Nullable
    public static final ItemType ORANGE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType ORANGE_SHULKER_BOX = init();
    @Nullable
    public static final ItemType ORANGE_STAINED_GLASS = init();
    @Nullable
    public static final ItemType ORANGE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType ORANGE_TERRACOTTA = init();
    @Nullable
    public static final ItemType ORANGE_TULIP = init();
    @Nullable
    public static final ItemType ORANGE_WOOL = init();
    @Nullable
    public static final ItemType OXEYE_DAISY = init();
    @Nullable
    public static final ItemType OXIDIZED_COPPER = init();
    @Nullable
    public static final ItemType OXIDIZED_CUT_COPPER = init();
    @Nullable
    public static final ItemType OXIDIZED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType OXIDIZED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType PACKED_ICE = init();
    @Nullable
    public static final ItemType PAINTING = init();
    @Nullable
    public static final ItemType PANDA_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PAPER = init();
    @Nullable
    public static final ItemType PARROT_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PEONY = init();
    @Nullable
    public static final ItemType PETRIFIED_OAK_SLAB = init();
    @Nullable
    public static final ItemType PHANTOM_MEMBRANE = init();
    @Nullable
    public static final ItemType PHANTOM_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PIG_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PIGLIN_BANNER_PATTERN = init();
    @Nullable
    public static final ItemType PIGLIN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PILLAGER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PINK_BANNER = init();
    @Nullable
    public static final ItemType PINK_BED = init();
    @Nullable
    public static final ItemType PINK_CANDLE = init();
    @Nullable
    public static final ItemType PINK_CARPET = init();
    @Nullable
    public static final ItemType PINK_CONCRETE = init();
    @Nullable
    public static final ItemType PINK_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType PINK_DYE = init();
    @Nullable
    public static final ItemType PINK_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType PINK_SHULKER_BOX = init();
    @Nullable
    public static final ItemType PINK_STAINED_GLASS = init();
    @Nullable
    public static final ItemType PINK_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType PINK_TERRACOTTA = init();
    @Nullable
    public static final ItemType PINK_TULIP = init();
    @Nullable
    public static final ItemType PINK_WOOL = init();
    @Nullable
    public static final ItemType PISTON = init();
    @Nullable
    public static final ItemType PLAYER_HEAD = init();
    @Nullable
    public static final ItemType PODZOL = init();
    @Nullable
    public static final ItemType POINTED_DRIPSTONE = init();
    @Nullable
    public static final ItemType POISONOUS_POTATO = init();
    @Nullable
    public static final ItemType POLAR_BEAR_SPAWN_EGG = init();
    @Nullable
    public static final ItemType POLISHED_ANDESITE = init();
    @Nullable
    public static final ItemType POLISHED_ANDESITE_SLAB = init();
    @Nullable
    public static final ItemType POLISHED_ANDESITE_STAIRS = init();
    @Nullable
    public static final ItemType POLISHED_BASALT = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_BRICK_SLAB = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_BRICK_WALL = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_BRICKS = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_BUTTON = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_SLAB = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_STAIRS = init();
    @Nullable
    public static final ItemType POLISHED_BLACKSTONE_WALL = init();
    @Nullable
    public static final ItemType POLISHED_DEEPSLATE = init();
    @Nullable
    public static final ItemType POLISHED_DEEPSLATE_SLAB = init();
    @Nullable
    public static final ItemType POLISHED_DEEPSLATE_STAIRS = init();
    @Nullable
    public static final ItemType POLISHED_DEEPSLATE_WALL = init();
    @Nullable
    public static final ItemType POLISHED_DIORITE = init();
    @Nullable
    public static final ItemType POLISHED_DIORITE_SLAB = init();
    @Nullable
    public static final ItemType POLISHED_DIORITE_STAIRS = init();
    @Nullable
    public static final ItemType POLISHED_GRANITE = init();
    @Nullable
    public static final ItemType POLISHED_GRANITE_SLAB = init();
    @Nullable
    public static final ItemType POLISHED_GRANITE_STAIRS = init();
    @Nullable
    public static final ItemType POPPED_CHORUS_FRUIT = init();
    @Nullable
    public static final ItemType POPPY = init();
    @Nullable
    public static final ItemType PORKCHOP = init();
    @Nullable
    public static final ItemType POTATO = init();
    @Nullable
    public static final ItemType POTION = init();
    @Nullable
    public static final ItemType POWDER_SNOW_BUCKET = init();
    @Nullable
    public static final ItemType POWERED_RAIL = init();
    @Nullable
    public static final ItemType PRISMARINE = init();
    @Nullable
    public static final ItemType PRISMARINE_BRICK_SLAB = init();
    @Nullable
    public static final ItemType PRISMARINE_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType PRISMARINE_BRICKS = init();
    @Nullable
    public static final ItemType PRISMARINE_CRYSTALS = init();
    @Nullable
    public static final ItemType PRISMARINE_SHARD = init();
    @Nullable
    public static final ItemType PRISMARINE_SLAB = init();
    @Nullable
    public static final ItemType PRISMARINE_STAIRS = init();
    @Nullable
    public static final ItemType PRISMARINE_WALL = init();
    @Nullable
    public static final ItemType PUFFERFISH = init();
    @Nullable
    public static final ItemType PUFFERFISH_BUCKET = init();
    @Nullable
    public static final ItemType PUFFERFISH_SPAWN_EGG = init();
    @Nullable
    public static final ItemType PUMPKIN = init();
    @Nullable
    public static final ItemType PUMPKIN_PIE = init();
    @Nullable
    public static final ItemType PUMPKIN_SEEDS = init();
    @Nullable
    public static final ItemType PURPLE_BANNER = init();
    @Nullable
    public static final ItemType PURPLE_BED = init();
    @Nullable
    public static final ItemType PURPLE_CANDLE = init();
    @Nullable
    public static final ItemType PURPLE_CARPET = init();
    @Nullable
    public static final ItemType PURPLE_CONCRETE = init();
    @Nullable
    public static final ItemType PURPLE_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType PURPLE_DYE = init();
    @Nullable
    public static final ItemType PURPLE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType PURPLE_SHULKER_BOX = init();
    @Nullable
    public static final ItemType PURPLE_STAINED_GLASS = init();
    @Nullable
    public static final ItemType PURPLE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType PURPLE_TERRACOTTA = init();
    @Nullable
    public static final ItemType PURPLE_WOOL = init();
    @Nullable
    public static final ItemType PURPUR_BLOCK = init();
    @Nullable
    public static final ItemType PURPUR_PILLAR = init();
    @Nullable
    public static final ItemType PURPUR_SLAB = init();
    @Nullable
    public static final ItemType PURPUR_STAIRS = init();
    @Nullable
    public static final ItemType QUARTZ = init();
    @Nullable
    public static final ItemType QUARTZ_BLOCK = init();
    @Nullable
    public static final ItemType QUARTZ_BRICKS = init();
    @Nullable
    public static final ItemType QUARTZ_PILLAR = init();
    @Nullable
    public static final ItemType QUARTZ_SLAB = init();
    @Nullable
    public static final ItemType QUARTZ_STAIRS = init();
    @Nullable
    public static final ItemType RABBIT = init();
    @Nullable
    public static final ItemType RABBIT_FOOT = init();
    @Nullable
    public static final ItemType RABBIT_HIDE = init();
    @Nullable
    public static final ItemType RABBIT_SPAWN_EGG = init();
    @Nullable
    public static final ItemType RABBIT_STEW = init();
    @Nullable
    public static final ItemType RAIL = init();
    @Nullable
    public static final ItemType RAVAGER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType RAW_COPPER = init();
    @Nullable
    public static final ItemType RAW_COPPER_BLOCK = init();
    @Nullable
    public static final ItemType RAW_GOLD = init();
    @Nullable
    public static final ItemType RAW_GOLD_BLOCK = init();
    @Nullable
    public static final ItemType RAW_IRON = init();
    @Nullable
    public static final ItemType RAW_IRON_BLOCK = init();
    @Nullable
    public static final ItemType RED_BANNER = init();
    @Nullable
    public static final ItemType RED_BED = init();
    @Nullable
    public static final ItemType RED_CANDLE = init();
    @Nullable
    public static final ItemType RED_CARPET = init();
    @Nullable
    public static final ItemType RED_CONCRETE = init();
    @Nullable
    public static final ItemType RED_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType RED_DYE = init();
    @Nullable
    public static final ItemType RED_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType RED_MUSHROOM = init();
    @Nullable
    public static final ItemType RED_MUSHROOM_BLOCK = init();
    @Nullable
    public static final ItemType RED_NETHER_BRICK_SLAB = init();
    @Nullable
    public static final ItemType RED_NETHER_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType RED_NETHER_BRICK_WALL = init();
    @Nullable
    public static final ItemType RED_NETHER_BRICKS = init();
    @Nullable
    public static final ItemType RED_SAND = init();
    @Nullable
    public static final ItemType RED_SANDSTONE = init();
    @Nullable
    public static final ItemType RED_SANDSTONE_SLAB = init();
    @Nullable
    public static final ItemType RED_SANDSTONE_STAIRS = init();
    @Nullable
    public static final ItemType RED_SANDSTONE_WALL = init();
    @Nullable
    public static final ItemType RED_SHULKER_BOX = init();
    @Nullable
    public static final ItemType RED_STAINED_GLASS = init();
    @Nullable
    public static final ItemType RED_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType RED_TERRACOTTA = init();
    @Nullable
    public static final ItemType RED_TULIP = init();
    @Nullable
    public static final ItemType RED_WOOL = init();
    @Nullable
    public static final ItemType REDSTONE = init();
    @Nullable
    public static final ItemType REDSTONE_BLOCK = init();
    @Nullable
    public static final ItemType REDSTONE_LAMP = init();
    @Nullable
    public static final ItemType REDSTONE_ORE = init();
    @Nullable
    public static final ItemType REDSTONE_TORCH = init();
    @Nullable
    public static final ItemType REPEATER = init();
    @Nullable
    public static final ItemType REPEATING_COMMAND_BLOCK = init();
    @Nullable
    public static final ItemType RESPAWN_ANCHOR = init();
    @Nullable
    public static final ItemType ROOTED_DIRT = init();
    @Nullable
    public static final ItemType ROSE_BUSH = init();
    @Deprecated
    @Nullable
    public static final ItemType ROSE_RED = init();
    @Nullable
    public static final ItemType ROTTEN_FLESH = init();
    @Nullable
    public static final ItemType SADDLE = init();
    @Nullable
    public static final ItemType SALMON = init();
    @Nullable
    public static final ItemType SALMON_BUCKET = init();
    @Nullable
    public static final ItemType SALMON_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SAND = init();
    @Nullable
    public static final ItemType SANDSTONE = init();
    @Nullable
    public static final ItemType SANDSTONE_SLAB = init();
    @Nullable
    public static final ItemType SANDSTONE_STAIRS = init();
    @Nullable
    public static final ItemType SANDSTONE_WALL = init();
    @Nullable
    public static final ItemType SCAFFOLDING = init();
    @Nullable
    public static final ItemType SCULK_SENSOR = init();
    @Nullable
    public static final ItemType SCUTE = init();
    @Nullable
    public static final ItemType SEA_LANTERN = init();
    @Nullable
    public static final ItemType SEA_PICKLE = init();
    @Nullable
    public static final ItemType SEAGRASS = init();
    @Nullable
    public static final ItemType SHEARS = init();
    @Nullable
    public static final ItemType SHEEP_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SHIELD = init();
    @Nullable
    public static final ItemType SHROOMLIGHT = init();
    @Nullable
    public static final ItemType SHULKER_BOX = init();
    @Nullable
    public static final ItemType SHULKER_SHELL = init();
    @Nullable
    public static final ItemType SHULKER_SPAWN_EGG = init();
    @Deprecated
    @Nullable
    public static final ItemType SIGN = init();
    @Nullable
    public static final ItemType SILVERFISH_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SKELETON_HORSE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SKELETON_SKULL = init();
    @Nullable
    public static final ItemType SKELETON_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SKULL_BANNER_PATTERN = init();
    @Nullable
    public static final ItemType SLIME_BALL = init();
    @Nullable
    public static final ItemType SLIME_BLOCK = init();
    @Nullable
    public static final ItemType SLIME_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SMALL_AMETHYST_BUD = init();
    @Nullable
    public static final ItemType SMALL_DRIPLEAF = init();
    @Nullable
    public static final ItemType SMITHING_TABLE = init();
    @Nullable
    public static final ItemType SMOKER = init();
    @Nullable
    public static final ItemType SMOOTH_BASALT = init();
    @Nullable
    public static final ItemType SMOOTH_QUARTZ = init();
    @Nullable
    public static final ItemType SMOOTH_QUARTZ_SLAB = init();
    @Nullable
    public static final ItemType SMOOTH_QUARTZ_STAIRS = init();
    @Nullable
    public static final ItemType SMOOTH_RED_SANDSTONE = init();
    @Nullable
    public static final ItemType SMOOTH_RED_SANDSTONE_SLAB = init();
    @Nullable
    public static final ItemType SMOOTH_RED_SANDSTONE_STAIRS = init();
    @Nullable
    public static final ItemType SMOOTH_SANDSTONE = init();
    @Nullable
    public static final ItemType SMOOTH_SANDSTONE_SLAB = init();
    @Nullable
    public static final ItemType SMOOTH_SANDSTONE_STAIRS = init();
    @Nullable
    public static final ItemType SMOOTH_STONE = init();
    @Nullable
    public static final ItemType SMOOTH_STONE_SLAB = init();
    @Nullable
    public static final ItemType SNOW = init();
    @Nullable
    public static final ItemType SNOW_BLOCK = init();
    @Nullable
    public static final ItemType SNOWBALL = init();
    @Nullable
    public static final ItemType SOUL_CAMPFIRE = init();
    @Nullable
    public static final ItemType SOUL_LANTERN = init();
    @Nullable
    public static final ItemType SOUL_SAND = init();
    @Nullable
    public static final ItemType SOUL_SOIL = init();
    @Nullable
    public static final ItemType SOUL_TORCH = init();
    @Nullable
    public static final ItemType SPAWNER = init();
    @Nullable
    public static final ItemType SPECTRAL_ARROW = init();
    @Nullable
    public static final ItemType SPIDER_EYE = init();
    @Nullable
    public static final ItemType SPIDER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType SPLASH_POTION = init();
    @Nullable
    public static final ItemType SPONGE = init();
    @Nullable
    public static final ItemType SPORE_BLOSSOM = init();
    @Nullable
    public static final ItemType SPRUCE_BOAT = init();
    @Nullable
    public static final ItemType SPRUCE_BUTTON = init();
    @Nullable
    public static final ItemType SPRUCE_DOOR = init();
    @Nullable
    public static final ItemType SPRUCE_FENCE = init();
    @Nullable
    public static final ItemType SPRUCE_FENCE_GATE = init();
    @Nullable
    public static final ItemType SPRUCE_LEAVES = init();
    @Nullable
    public static final ItemType SPRUCE_LOG = init();
    @Nullable
    public static final ItemType SPRUCE_PLANKS = init();
    @Nullable
    public static final ItemType SPRUCE_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType SPRUCE_SAPLING = init();
    @Nullable
    public static final ItemType SPRUCE_SIGN = init();
    @Nullable
    public static final ItemType SPRUCE_SLAB = init();
    @Nullable
    public static final ItemType SPRUCE_STAIRS = init();
    @Nullable
    public static final ItemType SPRUCE_TRAPDOOR = init();
    @Nullable
    public static final ItemType SPRUCE_WOOD = init();
    @Nullable
    public static final ItemType SPYGLASS = init();
    @Nullable
    public static final ItemType SQUID_SPAWN_EGG = init();
    @Nullable
    public static final ItemType STICK = init();
    @Nullable
    public static final ItemType STICKY_PISTON = init();
    @Nullable
    public static final ItemType STONE = init();
    @Nullable
    public static final ItemType STONE_AXE = init();
    @Nullable
    public static final ItemType STONE_BRICK_SLAB = init();
    @Nullable
    public static final ItemType STONE_BRICK_STAIRS = init();
    @Nullable
    public static final ItemType STONE_BRICK_WALL = init();
    @Nullable
    public static final ItemType STONE_BRICKS = init();
    @Nullable
    public static final ItemType STONE_BUTTON = init();
    @Nullable
    public static final ItemType STONE_HOE = init();
    @Nullable
    public static final ItemType STONE_PICKAXE = init();
    @Nullable
    public static final ItemType STONE_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType STONE_SHOVEL = init();
    @Nullable
    public static final ItemType STONE_SLAB = init();
    @Nullable
    public static final ItemType STONE_STAIRS = init();
    @Nullable
    public static final ItemType STONE_SWORD = init();
    @Nullable
    public static final ItemType STONECUTTER = init();
    @Nullable
    public static final ItemType STRAY_SPAWN_EGG = init();
    @Nullable
    public static final ItemType STRIDER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType STRING = init();
    @Nullable
    public static final ItemType STRIPPED_ACACIA_LOG = init();
    @Nullable
    public static final ItemType STRIPPED_ACACIA_WOOD = init();
    @Nullable
    public static final ItemType STRIPPED_BIRCH_LOG = init();
    @Nullable
    public static final ItemType STRIPPED_BIRCH_WOOD = init();
    @Nullable
    public static final ItemType STRIPPED_CRIMSON_HYPHAE = init();
    @Nullable
    public static final ItemType STRIPPED_CRIMSON_STEM = init();
    @Nullable
    public static final ItemType STRIPPED_DARK_OAK_LOG = init();
    @Nullable
    public static final ItemType STRIPPED_DARK_OAK_WOOD = init();
    @Nullable
    public static final ItemType STRIPPED_JUNGLE_LOG = init();
    @Nullable
    public static final ItemType STRIPPED_JUNGLE_WOOD = init();
    @Nullable
    public static final ItemType STRIPPED_OAK_LOG = init();
    @Nullable
    public static final ItemType STRIPPED_OAK_WOOD = init();
    @Nullable
    public static final ItemType STRIPPED_SPRUCE_LOG = init();
    @Nullable
    public static final ItemType STRIPPED_SPRUCE_WOOD = init();
    @Nullable
    public static final ItemType STRIPPED_WARPED_HYPHAE = init();
    @Nullable
    public static final ItemType STRIPPED_WARPED_STEM = init();
    @Nullable
    public static final ItemType STRUCTURE_BLOCK = init();
    @Nullable
    public static final ItemType STRUCTURE_VOID = init();
    @Nullable
    public static final ItemType SUGAR = init();
    @Nullable
    public static final ItemType SUGAR_CANE = init();
    @Nullable
    public static final ItemType SUNFLOWER = init();
    @Nullable
    public static final ItemType SUSPICIOUS_STEW = init();
    @Nullable
    public static final ItemType SWEET_BERRIES = init();
    @Nullable
    public static final ItemType TALL_GRASS = init();
    @Nullable
    public static final ItemType TARGET = init();
    @Nullable
    public static final ItemType TERRACOTTA = init();
    @Nullable
    public static final ItemType TINTED_GLASS = init();
    @Nullable
    public static final ItemType TIPPED_ARROW = init();
    @Nullable
    public static final ItemType TNT = init();
    @Nullable
    public static final ItemType TNT_MINECART = init();
    @Nullable
    public static final ItemType TORCH = init();
    @Nullable
    public static final ItemType TOTEM_OF_UNDYING = init();
    @Nullable
    public static final ItemType TRADER_LLAMA_SPAWN_EGG = init();
    @Nullable
    public static final ItemType TRAPPED_CHEST = init();
    @Nullable
    public static final ItemType TRIDENT = init();
    @Nullable
    public static final ItemType TRIPWIRE_HOOK = init();
    @Nullable
    public static final ItemType TROPICAL_FISH = init();
    @Nullable
    public static final ItemType TROPICAL_FISH_BUCKET = init();
    @Nullable
    public static final ItemType TROPICAL_FISH_SPAWN_EGG = init();
    @Nullable
    public static final ItemType TUBE_CORAL = init();
    @Nullable
    public static final ItemType TUBE_CORAL_BLOCK = init();
    @Nullable
    public static final ItemType TUBE_CORAL_FAN = init();
    @Nullable
    public static final ItemType TUFF = init();
    @Nullable
    public static final ItemType TURTLE_EGG = init();
    @Nullable
    public static final ItemType TURTLE_HELMET = init();
    @Nullable
    public static final ItemType TURTLE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType TWISTING_VINES = init();
    @Nullable
    public static final ItemType VEX_SPAWN_EGG = init();
    @Nullable
    public static final ItemType VILLAGER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType VINDICATOR_SPAWN_EGG = init();
    @Nullable
    public static final ItemType VINE = init();
    @Nullable
    public static final ItemType WANDERING_TRADER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType WARPED_BUTTON = init();
    @Nullable
    public static final ItemType WARPED_DOOR = init();
    @Nullable
    public static final ItemType WARPED_FENCE = init();
    @Nullable
    public static final ItemType WARPED_FENCE_GATE = init();
    @Nullable
    public static final ItemType WARPED_FUNGUS = init();
    @Nullable
    public static final ItemType WARPED_FUNGUS_ON_A_STICK = init();
    @Nullable
    public static final ItemType WARPED_HYPHAE = init();
    @Nullable
    public static final ItemType WARPED_NYLIUM = init();
    @Nullable
    public static final ItemType WARPED_PLANKS = init();
    @Nullable
    public static final ItemType WARPED_PRESSURE_PLATE = init();
    @Nullable
    public static final ItemType WARPED_ROOTS = init();
    @Nullable
    public static final ItemType WARPED_SIGN = init();
    @Nullable
    public static final ItemType WARPED_SLAB = init();
    @Nullable
    public static final ItemType WARPED_STAIRS = init();
    @Nullable
    public static final ItemType WARPED_STEM = init();
    @Nullable
    public static final ItemType WARPED_TRAPDOOR = init();
    @Nullable
    public static final ItemType WARPED_WART_BLOCK = init();
    @Nullable
    public static final ItemType WATER_BUCKET = init();
    @Nullable
    public static final ItemType WAXED_COPPER_BLOCK = init();
    @Nullable
    public static final ItemType WAXED_CUT_COPPER = init();
    @Nullable
    public static final ItemType WAXED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType WAXED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType WAXED_EXPOSED_COPPER = init();
    @Nullable
    public static final ItemType WAXED_EXPOSED_CUT_COPPER = init();
    @Nullable
    public static final ItemType WAXED_EXPOSED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType WAXED_EXPOSED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType WAXED_OXIDIZED_COPPER = init();
    @Nullable
    public static final ItemType WAXED_OXIDIZED_CUT_COPPER = init();
    @Nullable
    public static final ItemType WAXED_OXIDIZED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType WAXED_OXIDIZED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType WAXED_WEATHERED_COPPER = init();
    @Nullable
    public static final ItemType WAXED_WEATHERED_CUT_COPPER = init();
    @Nullable
    public static final ItemType WAXED_WEATHERED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType WAXED_WEATHERED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType WEATHERED_COPPER = init();
    @Nullable
    public static final ItemType WEATHERED_CUT_COPPER = init();
    @Nullable
    public static final ItemType WEATHERED_CUT_COPPER_SLAB = init();
    @Nullable
    public static final ItemType WEATHERED_CUT_COPPER_STAIRS = init();
    @Nullable
    public static final ItemType WEEPING_VINES = init();
    @Nullable
    public static final ItemType WET_SPONGE = init();
    @Nullable
    public static final ItemType WHEAT = init();
    @Nullable
    public static final ItemType WHEAT_SEEDS = init();
    @Nullable
    public static final ItemType WHITE_BANNER = init();
    @Nullable
    public static final ItemType WHITE_BED = init();
    @Nullable
    public static final ItemType WHITE_CANDLE = init();
    @Nullable
    public static final ItemType WHITE_CARPET = init();
    @Nullable
    public static final ItemType WHITE_CONCRETE = init();
    @Nullable
    public static final ItemType WHITE_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType WHITE_DYE = init();
    @Nullable
    public static final ItemType WHITE_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType WHITE_SHULKER_BOX = init();
    @Nullable
    public static final ItemType WHITE_STAINED_GLASS = init();
    @Nullable
    public static final ItemType WHITE_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType WHITE_TERRACOTTA = init();
    @Nullable
    public static final ItemType WHITE_TULIP = init();
    @Nullable
    public static final ItemType WHITE_WOOL = init();
    @Nullable
    public static final ItemType WITCH_SPAWN_EGG = init();
    @Nullable
    public static final ItemType WITHER_ROSE = init();
    @Nullable
    public static final ItemType WITHER_SKELETON_SKULL = init();
    @Nullable
    public static final ItemType WITHER_SKELETON_SPAWN_EGG = init();
    @Nullable
    public static final ItemType WOLF_SPAWN_EGG = init();
    @Nullable
    public static final ItemType WOODEN_AXE = init();
    @Nullable
    public static final ItemType WOODEN_HOE = init();
    @Nullable
    public static final ItemType WOODEN_PICKAXE = init();
    @Nullable
    public static final ItemType WOODEN_SHOVEL = init();
    @Nullable
    public static final ItemType WOODEN_SWORD = init();
    @Nullable
    public static final ItemType WRITABLE_BOOK = init();
    @Nullable
    public static final ItemType WRITTEN_BOOK = init();
    @Nullable
    public static final ItemType YELLOW_BANNER = init();
    @Nullable
    public static final ItemType YELLOW_BED = init();
    @Nullable
    public static final ItemType YELLOW_CANDLE = init();
    @Nullable
    public static final ItemType YELLOW_CARPET = init();
    @Nullable
    public static final ItemType YELLOW_CONCRETE = init();
    @Nullable
    public static final ItemType YELLOW_CONCRETE_POWDER = init();
    @Nullable
    public static final ItemType YELLOW_DYE = init();
    @Nullable
    public static final ItemType YELLOW_GLAZED_TERRACOTTA = init();
    @Nullable
    public static final ItemType YELLOW_SHULKER_BOX = init();
    @Nullable
    public static final ItemType YELLOW_STAINED_GLASS = init();
    @Nullable
    public static final ItemType YELLOW_STAINED_GLASS_PANE = init();
    @Nullable
    public static final ItemType YELLOW_TERRACOTTA = init();
    @Nullable
    public static final ItemType YELLOW_WOOL = init();
    @Nullable
    public static final ItemType ZOGLIN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ZOMBIE_HEAD = init();
    @Nullable
    public static final ItemType ZOMBIE_HORSE_SPAWN_EGG = init();
    @Deprecated
    @Nullable
    public static final ItemType ZOMBIE_PIGMAN_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ZOMBIE_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ZOMBIE_VILLAGER_SPAWN_EGG = init();
    @Nullable
    public static final ItemType ZOMBIFIED_PIGLIN_SPAWN_EGG = init();

    private ItemTypes() {
    }

    private static Field[] fieldsTmp;
    private static JoinedCharSequence joined;
    private static int initIndex = 0;

    private static ItemType init() {
        try {
            if (fieldsTmp == null) {
                fieldsTmp = ItemTypes.class.getDeclaredFields();
                ItemTypesCache.init(); // force class to load
                joined = new JoinedCharSequence();
            }
            String name = fieldsTmp[initIndex++].getName().toLowerCase(Locale.ROOT);
            CharSequence fullName = joined.init(ItemType.REGISTRY.getDefaultNamespace(), ':', name);
            return ItemType.REGISTRY.getMap().get(fullName);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    static {
        fieldsTmp = null;
        joined = null;
    }

    @Nullable
    public static ItemType parse(String input) {
        input = input.toLowerCase(Locale.ROOT);
        if (!Character.isAlphabetic(input.charAt(0))) {
            try {
                ItemType legacy = LegacyMapper.getInstance().getItemFromLegacy(input);
                if (legacy != null) {
                    return legacy;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (!input.split("\\[", 2)[0].contains(":")) {
            input = "minecraft:" + input;
        }
        return get(input);
    }
    //FAWE end

    /**
     * Gets the {@link ItemType} associated with the given id.
     */
    @Nullable
    public static ItemType get(String id) {
        return ItemType.REGISTRY.get(id);
    }

    //FAWE start
    @Deprecated
    public static ItemType get(int ordinal) {
        return ItemType.REGISTRY.getByInternalId(ordinal);
    }

    public static int size() {
        return ItemType.REGISTRY.size();
    }

    public static Collection<ItemType> values() {
        return ItemType.REGISTRY.values();
    }
    //FAWE end
}
