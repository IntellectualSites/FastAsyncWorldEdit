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
package com.sk89q.worldedit.world.item;

import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.registry.NamespacedRegistry;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BundledItemData;
import com.sk89q.worldedit.world.registry.LegacyMapper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum ItemTypes implements ItemType {
    /*
     -----------------------------------------------------
        Replaced at runtime by the item registry
     -----------------------------------------------------
     */

    ACACIA_BOAT,
    ACACIA_BUTTON,
    ACACIA_DOOR,
    ACACIA_FENCE,
    ACACIA_FENCE_GATE,
    ACACIA_LEAVES,
    ACACIA_LOG,
    ACACIA_PLANKS,
    ACACIA_PRESSURE_PLATE,
    ACACIA_SAPLING,
    ACACIA_SLAB,
    ACACIA_STAIRS,
    ACACIA_TRAPDOOR,
    ACACIA_WOOD,
    ACTIVATOR_RAIL,
    AIR,
    ALLIUM,
    ANDESITE,
    ANVIL,
    APPLE,
    ARMOR_STAND,
    ARROW,
    AZURE_BLUET,
    BAKED_POTATO,
    BARRIER,
    BAT_SPAWN_EGG,
    BEACON,
    BEDROCK,
    BEEF,
    BEETROOT,
    BEETROOT_SEEDS,
    BEETROOT_SOUP,
    BIRCH_BOAT,
    BIRCH_BUTTON,
    BIRCH_DOOR,
    BIRCH_FENCE,
    BIRCH_FENCE_GATE,
    BIRCH_LEAVES,
    BIRCH_LOG,
    BIRCH_PLANKS,
    BIRCH_PRESSURE_PLATE,
    BIRCH_SAPLING,
    BIRCH_SLAB,
    BIRCH_STAIRS,
    BIRCH_TRAPDOOR,
    BIRCH_WOOD,
    BLACK_BANNER,
    BLACK_BED,
    BLACK_CARPET,
    BLACK_CONCRETE,
    BLACK_CONCRETE_POWDER,
    BLACK_GLAZED_TERRACOTTA,
    BLACK_SHULKER_BOX,
    BLACK_STAINED_GLASS,
    BLACK_STAINED_GLASS_PANE,
    BLACK_TERRACOTTA,
    BLACK_WOOL,
    BLAZE_POWDER,
    BLAZE_ROD,
    BLAZE_SPAWN_EGG,
    BLUE_BANNER,
    BLUE_BED,
    BLUE_CARPET,
    BLUE_CONCRETE,
    BLUE_CONCRETE_POWDER,
    BLUE_GLAZED_TERRACOTTA,
    BLUE_ICE,
    BLUE_ORCHID,
    BLUE_SHULKER_BOX,
    BLUE_STAINED_GLASS,
    BLUE_STAINED_GLASS_PANE,
    BLUE_TERRACOTTA,
    BLUE_WOOL,
    BONE,
    BONE_BLOCK,
    BONE_MEAL,
    BOOK,
    BOOKSHELF,
    BOW,
    BOWL,
    BRAIN_CORAL,
    BRAIN_CORAL_BLOCK,
    BRAIN_CORAL_FAN,
    BREAD,
    BREWING_STAND,
    BRICK,
    BRICKS,
    BRICK_SLAB,
    BRICK_STAIRS,
    BROWN_BANNER,
    BROWN_BED,
    BROWN_CARPET,
    BROWN_CONCRETE,
    BROWN_CONCRETE_POWDER,
    BROWN_GLAZED_TERRACOTTA,
    BROWN_MUSHROOM,
    BROWN_MUSHROOM_BLOCK,
    BROWN_SHULKER_BOX,
    BROWN_STAINED_GLASS,
    BROWN_STAINED_GLASS_PANE,
    BROWN_TERRACOTTA,
    BROWN_WOOL,
    BUBBLE_CORAL,
    BUBBLE_CORAL_BLOCK,
    BUBBLE_CORAL_FAN,
    BUCKET,
    CACTUS,
    CACTUS_GREEN,
    CAKE,
    CARROT,
    CARROT_ON_A_STICK,
    CARVED_PUMPKIN,
    CAULDRON,
    CAVE_SPIDER_SPAWN_EGG,
    CHAINMAIL_BOOTS,
    CHAINMAIL_CHESTPLATE,
    CHAINMAIL_HELMET,
    CHAINMAIL_LEGGINGS,
    CHAIN_COMMAND_BLOCK,
    CHARCOAL,
    CHEST,
    CHEST_MINECART,
    CHICKEN,
    CHICKEN_SPAWN_EGG,
    CHIPPED_ANVIL,
    CHISELED_QUARTZ_BLOCK,
    CHISELED_RED_SANDSTONE,
    CHISELED_SANDSTONE,
    CHISELED_STONE_BRICKS,
    CHORUS_FLOWER,
    CHORUS_FRUIT,
    CHORUS_PLANT,
    CLAY,
    CLAY_BALL,
    CLOCK,
    COAL,
    COAL_BLOCK,
    COAL_ORE,
    COARSE_DIRT,
    COBBLESTONE,
    COBBLESTONE_SLAB,
    COBBLESTONE_STAIRS,
    COBBLESTONE_WALL,
    COBWEB,
    COCOA_BEANS,
    COD,
    COD_BUCKET,
    COD_SPAWN_EGG,
    COMMAND_BLOCK,
    COMMAND_BLOCK_MINECART,
    COMPARATOR,
    COMPASS,
    CONDUIT,
    COOKED_BEEF,
    COOKED_CHICKEN,
    COOKED_COD,
    COOKED_MUTTON,
    COOKED_PORKCHOP,
    COOKED_RABBIT,
    COOKED_SALMON,
    COOKIE,
    COW_SPAWN_EGG,
    CRACKED_STONE_BRICKS,
    CRAFTING_TABLE,
    CREEPER_HEAD,
    CREEPER_SPAWN_EGG,
    CUT_RED_SANDSTONE,
    CUT_SANDSTONE,
    CYAN_BANNER,
    CYAN_BED,
    CYAN_CARPET,
    CYAN_CONCRETE,
    CYAN_CONCRETE_POWDER,
    CYAN_DYE,
    CYAN_GLAZED_TERRACOTTA,
    CYAN_SHULKER_BOX,
    CYAN_STAINED_GLASS,
    CYAN_STAINED_GLASS_PANE,
    CYAN_TERRACOTTA,
    CYAN_WOOL,
    DAMAGED_ANVIL,
    DANDELION,
    DANDELION_YELLOW,
    DARK_OAK_BOAT,
    DARK_OAK_BUTTON,
    DARK_OAK_DOOR,
    DARK_OAK_FENCE,
    DARK_OAK_FENCE_GATE,
    DARK_OAK_LEAVES,
    DARK_OAK_LOG,
    DARK_OAK_PLANKS,
    DARK_OAK_PRESSURE_PLATE,
    DARK_OAK_SAPLING,
    DARK_OAK_SLAB,
    DARK_OAK_STAIRS,
    DARK_OAK_TRAPDOOR,
    DARK_OAK_WOOD,
    DARK_PRISMARINE,
    DARK_PRISMARINE_SLAB,
    DARK_PRISMARINE_STAIRS,
    DAYLIGHT_DETECTOR,
    DEAD_BRAIN_CORAL_BLOCK,
    DEAD_BRAIN_CORAL_FAN,
    DEAD_BUBBLE_CORAL_BLOCK,
    DEAD_BUBBLE_CORAL_FAN,
    DEAD_BUSH,
    DEAD_FIRE_CORAL_BLOCK,
    DEAD_FIRE_CORAL_FAN,
    DEAD_HORN_CORAL_BLOCK,
    DEAD_HORN_CORAL_FAN,
    DEAD_TUBE_CORAL_BLOCK,
    DEAD_TUBE_CORAL_FAN,
    DEBUG_STICK,
    DETECTOR_RAIL,
    DIAMOND,
    DIAMOND_AXE,
    DIAMOND_BLOCK,
    DIAMOND_BOOTS,
    DIAMOND_CHESTPLATE,
    DIAMOND_HELMET,
    DIAMOND_HOE,
    DIAMOND_HORSE_ARMOR,
    DIAMOND_LEGGINGS,
    DIAMOND_ORE,
    DIAMOND_PICKAXE,
    DIAMOND_SHOVEL,
    DIAMOND_SWORD,
    DIORITE,
    DIRT,
    DISPENSER,
    DOLPHIN_SPAWN_EGG,
    DONKEY_SPAWN_EGG,
    DRAGON_BREATH,
    DRAGON_EGG,
    DRAGON_HEAD,
    DRIED_KELP,
    DRIED_KELP_BLOCK,
    DROPPER,
    DROWNED_SPAWN_EGG,
    EGG,
    ELDER_GUARDIAN_SPAWN_EGG,
    ELYTRA,
    EMERALD,
    EMERALD_BLOCK,
    EMERALD_ORE,
    ENCHANTED_BOOK,
    ENCHANTED_GOLDEN_APPLE,
    ENCHANTING_TABLE,
    ENDERMAN_SPAWN_EGG,
    ENDERMITE_SPAWN_EGG,
    ENDER_CHEST,
    ENDER_EYE,
    ENDER_PEARL,
    END_CRYSTAL,
    END_PORTAL_FRAME,
    END_ROD,
    END_STONE,
    END_STONE_BRICKS,
    EVOKER_SPAWN_EGG,
    EXPERIENCE_BOTTLE,
    FARMLAND,
    FEATHER,
    FERMENTED_SPIDER_EYE,
    FERN,
    FILLED_MAP,
    FIREWORK_ROCKET,
    FIREWORK_STAR,
    FIRE_CHARGE,
    FIRE_CORAL,
    FIRE_CORAL_BLOCK,
    FIRE_CORAL_FAN,
    FISHING_ROD,
    FLINT,
    FLINT_AND_STEEL,
    FLOWER_POT,
    FURNACE,
    FURNACE_MINECART,
    GHAST_SPAWN_EGG,
    GHAST_TEAR,
    GLASS,
    GLASS_BOTTLE,
    GLASS_PANE,
    GLISTERING_MELON_SLICE,
    GLOWSTONE,
    GLOWSTONE_DUST,
    GOLDEN_APPLE,
    GOLDEN_AXE,
    GOLDEN_BOOTS,
    GOLDEN_CARROT,
    GOLDEN_CHESTPLATE,
    GOLDEN_HELMET,
    GOLDEN_HOE,
    GOLDEN_HORSE_ARMOR,
    GOLDEN_LEGGINGS,
    GOLDEN_PICKAXE,
    GOLDEN_SHOVEL,
    GOLDEN_SWORD,
    GOLD_BLOCK,
    GOLD_INGOT,
    GOLD_NUGGET,
    GOLD_ORE,
    GRANITE,
    GRASS,
    GRASS_BLOCK,
    GRASS_PATH,
    GRAVEL,
    GRAY_BANNER,
    GRAY_BED,
    GRAY_CARPET,
    GRAY_CONCRETE,
    GRAY_CONCRETE_POWDER,
    GRAY_DYE,
    GRAY_GLAZED_TERRACOTTA,
    GRAY_SHULKER_BOX,
    GRAY_STAINED_GLASS,
    GRAY_STAINED_GLASS_PANE,
    GRAY_TERRACOTTA,
    GRAY_WOOL,
    GREEN_BANNER,
    GREEN_BED,
    GREEN_CARPET,
    GREEN_CONCRETE,
    GREEN_CONCRETE_POWDER,
    GREEN_GLAZED_TERRACOTTA,
    GREEN_SHULKER_BOX,
    GREEN_STAINED_GLASS,
    GREEN_STAINED_GLASS_PANE,
    GREEN_TERRACOTTA,
    GREEN_WOOL,
    GUARDIAN_SPAWN_EGG,
    GUNPOWDER,
    HAY_BLOCK,
    HEART_OF_THE_SEA,
    HEAVY_WEIGHTED_PRESSURE_PLATE,
    HOPPER,
    HOPPER_MINECART,
    HORN_CORAL,
    HORN_CORAL_BLOCK,
    HORN_CORAL_FAN,
    HORSE_SPAWN_EGG,
    HUSK_SPAWN_EGG,
    ICE,
    INFESTED_CHISELED_STONE_BRICKS,
    INFESTED_COBBLESTONE,
    INFESTED_CRACKED_STONE_BRICKS,
    INFESTED_MOSSY_STONE_BRICKS,
    INFESTED_STONE,
    INFESTED_STONE_BRICKS,
    INK_SAC,
    IRON_AXE,
    IRON_BARS,
    IRON_BLOCK,
    IRON_BOOTS,
    IRON_CHESTPLATE,
    IRON_DOOR,
    IRON_HELMET,
    IRON_HOE,
    IRON_HORSE_ARMOR,
    IRON_INGOT,
    IRON_LEGGINGS,
    IRON_NUGGET,
    IRON_ORE,
    IRON_PICKAXE,
    IRON_SHOVEL,
    IRON_SWORD,
    IRON_TRAPDOOR,
    ITEM_FRAME,
    JACK_O_LANTERN,
    JUKEBOX,
    JUNGLE_BOAT,
    JUNGLE_BUTTON,
    JUNGLE_DOOR,
    JUNGLE_FENCE,
    JUNGLE_FENCE_GATE,
    JUNGLE_LEAVES,
    JUNGLE_LOG,
    JUNGLE_PLANKS,
    JUNGLE_PRESSURE_PLATE,
    JUNGLE_SAPLING,
    JUNGLE_SLAB,
    JUNGLE_STAIRS,
    JUNGLE_TRAPDOOR,
    JUNGLE_WOOD,
    KELP,
    KNOWLEDGE_BOOK,
    LADDER,
    LAPIS_BLOCK,
    LAPIS_LAZULI,
    LAPIS_ORE,
    LARGE_FERN,
    LAVA_BUCKET,
    LEAD,
    LEATHER,
    LEATHER_BOOTS,
    LEATHER_CHESTPLATE,
    LEATHER_HELMET,
    LEATHER_LEGGINGS,
    LEVER,
    LIGHT_BLUE_BANNER,
    LIGHT_BLUE_BED,
    LIGHT_BLUE_CARPET,
    LIGHT_BLUE_CONCRETE,
    LIGHT_BLUE_CONCRETE_POWDER,
    LIGHT_BLUE_DYE,
    LIGHT_BLUE_GLAZED_TERRACOTTA,
    LIGHT_BLUE_SHULKER_BOX,
    LIGHT_BLUE_STAINED_GLASS,
    LIGHT_BLUE_STAINED_GLASS_PANE,
    LIGHT_BLUE_TERRACOTTA,
    LIGHT_BLUE_WOOL,
    LIGHT_GRAY_BANNER,
    LIGHT_GRAY_BED,
    LIGHT_GRAY_CARPET,
    LIGHT_GRAY_CONCRETE,
    LIGHT_GRAY_CONCRETE_POWDER,
    LIGHT_GRAY_DYE,
    LIGHT_GRAY_GLAZED_TERRACOTTA,
    LIGHT_GRAY_SHULKER_BOX,
    LIGHT_GRAY_STAINED_GLASS,
    LIGHT_GRAY_STAINED_GLASS_PANE,
    LIGHT_GRAY_TERRACOTTA,
    LIGHT_GRAY_WOOL,
    LIGHT_WEIGHTED_PRESSURE_PLATE,
    LILAC,
    LILY_PAD,
    LIME_BANNER,
    LIME_BED,
    LIME_CARPET,
    LIME_CONCRETE,
    LIME_CONCRETE_POWDER,
    LIME_DYE,
    LIME_GLAZED_TERRACOTTA,
    LIME_SHULKER_BOX,
    LIME_STAINED_GLASS,
    LIME_STAINED_GLASS_PANE,
    LIME_TERRACOTTA,
    LIME_WOOL,
    LINGERING_POTION,
    LLAMA_SPAWN_EGG,
    MAGENTA_BANNER,
    MAGENTA_BED,
    MAGENTA_CARPET,
    MAGENTA_CONCRETE,
    MAGENTA_CONCRETE_POWDER,
    MAGENTA_DYE,
    MAGENTA_GLAZED_TERRACOTTA,
    MAGENTA_SHULKER_BOX,
    MAGENTA_STAINED_GLASS,
    MAGENTA_STAINED_GLASS_PANE,
    MAGENTA_TERRACOTTA,
    MAGENTA_WOOL,
    MAGMA_BLOCK,
    MAGMA_CREAM,
    MAGMA_CUBE_SPAWN_EGG,
    MAP,
    MELON,
    MELON_SEEDS,
    MELON_SLICE,
    MILK_BUCKET,
    MINECART,
    MOOSHROOM_SPAWN_EGG,
    MOSSY_COBBLESTONE,
    MOSSY_COBBLESTONE_WALL,
    MOSSY_STONE_BRICKS,
    MULE_SPAWN_EGG,
    MUSHROOM_STEM,
    MUSHROOM_STEW,
    MUSIC_DISC_11,
    MUSIC_DISC_13,
    MUSIC_DISC_BLOCKS,
    MUSIC_DISC_CAT,
    MUSIC_DISC_CHIRP,
    MUSIC_DISC_FAR,
    MUSIC_DISC_MALL,
    MUSIC_DISC_MELLOHI,
    MUSIC_DISC_STAL,
    MUSIC_DISC_STRAD,
    MUSIC_DISC_WAIT,
    MUSIC_DISC_WARD,
    MUTTON,
    MYCELIUM,
    NAME_TAG,
    NAUTILUS_SHELL,
    NETHERRACK,
    NETHER_BRICK,
    NETHER_BRICKS,
    NETHER_BRICK_FENCE,
    NETHER_BRICK_SLAB,
    NETHER_BRICK_STAIRS,
    NETHER_QUARTZ_ORE,
    NETHER_STAR,
    NETHER_WART,
    NETHER_WART_BLOCK,
    NOTE_BLOCK,
    OAK_BOAT,
    OAK_BUTTON,
    OAK_DOOR,
    OAK_FENCE,
    OAK_FENCE_GATE,
    OAK_LEAVES,
    OAK_LOG,
    OAK_PLANKS,
    OAK_PRESSURE_PLATE,
    OAK_SAPLING,
    OAK_SLAB,
    OAK_STAIRS,
    OAK_TRAPDOOR,
    OAK_WOOD,
    OBSERVER,
    OBSIDIAN,
    OCELOT_SPAWN_EGG,
    ORANGE_BANNER,
    ORANGE_BED,
    ORANGE_CARPET,
    ORANGE_CONCRETE,
    ORANGE_CONCRETE_POWDER,
    ORANGE_DYE,
    ORANGE_GLAZED_TERRACOTTA,
    ORANGE_SHULKER_BOX,
    ORANGE_STAINED_GLASS,
    ORANGE_STAINED_GLASS_PANE,
    ORANGE_TERRACOTTA,
    ORANGE_TULIP,
    ORANGE_WOOL,
    OXEYE_DAISY,
    PACKED_ICE,
    PAINTING,
    PAPER,
    PARROT_SPAWN_EGG,
    PEONY,
    PETRIFIED_OAK_SLAB,
    PHANTOM_MEMBRANE,
    PHANTOM_SPAWN_EGG,
    PIG_SPAWN_EGG,
    PINK_BANNER,
    PINK_BED,
    PINK_CARPET,
    PINK_CONCRETE,
    PINK_CONCRETE_POWDER,
    PINK_DYE,
    PINK_GLAZED_TERRACOTTA,
    PINK_SHULKER_BOX,
    PINK_STAINED_GLASS,
    PINK_STAINED_GLASS_PANE,
    PINK_TERRACOTTA,
    PINK_TULIP,
    PINK_WOOL,
    PISTON,
    PLAYER_HEAD,
    PODZOL,
    POISONOUS_POTATO,
    POLAR_BEAR_SPAWN_EGG,
    POLISHED_ANDESITE,
    POLISHED_DIORITE,
    POLISHED_GRANITE,
    POPPED_CHORUS_FRUIT,
    POPPY,
    PORKCHOP,
    POTATO,
    POTION,
    POWERED_RAIL,
    PRISMARINE,
    PRISMARINE_BRICKS,
    PRISMARINE_BRICK_SLAB,
    PRISMARINE_BRICK_STAIRS,
    PRISMARINE_CRYSTALS,
    PRISMARINE_SHARD,
    PRISMARINE_SLAB,
    PRISMARINE_STAIRS,
    PUFFERFISH,
    PUFFERFISH_BUCKET,
    PUFFERFISH_SPAWN_EGG,
    PUMPKIN,
    PUMPKIN_PIE,
    PUMPKIN_SEEDS,
    PURPLE_BANNER,
    PURPLE_BED,
    PURPLE_CARPET,
    PURPLE_CONCRETE,
    PURPLE_CONCRETE_POWDER,
    PURPLE_DYE,
    PURPLE_GLAZED_TERRACOTTA,
    PURPLE_SHULKER_BOX,
    PURPLE_STAINED_GLASS,
    PURPLE_STAINED_GLASS_PANE,
    PURPLE_TERRACOTTA,
    PURPLE_WOOL,
    PURPUR_BLOCK,
    PURPUR_PILLAR,
    PURPUR_SLAB,
    PURPUR_STAIRS,
    QUARTZ,
    QUARTZ_BLOCK,
    QUARTZ_PILLAR,
    QUARTZ_SLAB,
    QUARTZ_STAIRS,
    RABBIT,
    RABBIT_FOOT,
    RABBIT_HIDE,
    RABBIT_SPAWN_EGG,
    RABBIT_STEW,
    RAIL,
    REDSTONE,
    REDSTONE_BLOCK,
    REDSTONE_LAMP,
    REDSTONE_ORE,
    REDSTONE_TORCH,
    RED_BANNER,
    RED_BED,
    RED_CARPET,
    RED_CONCRETE,
    RED_CONCRETE_POWDER,
    RED_GLAZED_TERRACOTTA,
    RED_MUSHROOM,
    RED_MUSHROOM_BLOCK,
    RED_NETHER_BRICKS,
    RED_SAND,
    RED_SANDSTONE,
    RED_SANDSTONE_SLAB,
    RED_SANDSTONE_STAIRS,
    RED_SHULKER_BOX,
    RED_STAINED_GLASS,
    RED_STAINED_GLASS_PANE,
    RED_TERRACOTTA,
    RED_TULIP,
    RED_WOOL,
    REPEATER,
    REPEATING_COMMAND_BLOCK,
    ROSE_BUSH,
    ROSE_RED,
    ROTTEN_FLESH,
    SADDLE,
    SALMON,
    SALMON_BUCKET,
    SALMON_SPAWN_EGG,
    SAND,
    SANDSTONE,
    SANDSTONE_SLAB,
    SANDSTONE_STAIRS,
    SCUTE,
    SEAGRASS,
    SEA_LANTERN,
    SEA_PICKLE,
    SHEARS,
    SHEEP_SPAWN_EGG,
    SHIELD,
    SHULKER_BOX,
    SHULKER_SHELL,
    SHULKER_SPAWN_EGG,
    SIGN,
    SILVERFISH_SPAWN_EGG,
    SKELETON_HORSE_SPAWN_EGG,
    SKELETON_SKULL,
    SKELETON_SPAWN_EGG,
    SLIME_BALL,
    SLIME_BLOCK,
    SLIME_SPAWN_EGG,
    SMOOTH_QUARTZ,
    SMOOTH_RED_SANDSTONE,
    SMOOTH_SANDSTONE,
    SMOOTH_STONE,
    SNOW,
    SNOWBALL,
    SNOW_BLOCK,
    SOUL_SAND,
    SPAWNER,
    SPECTRAL_ARROW,
    SPIDER_EYE,
    SPIDER_SPAWN_EGG,
    SPLASH_POTION,
    SPONGE,
    SPRUCE_BOAT,
    SPRUCE_BUTTON,
    SPRUCE_DOOR,
    SPRUCE_FENCE,
    SPRUCE_FENCE_GATE,
    SPRUCE_LEAVES,
    SPRUCE_LOG,
    SPRUCE_PLANKS,
    SPRUCE_PRESSURE_PLATE,
    SPRUCE_SAPLING,
    SPRUCE_SLAB,
    SPRUCE_STAIRS,
    SPRUCE_TRAPDOOR,
    SPRUCE_WOOD,
    SQUID_SPAWN_EGG,
    STICK,
    STICKY_PISTON,
    STONE,
    STONE_AXE,
    STONE_BRICKS,
    STONE_BRICK_SLAB,
    STONE_BRICK_STAIRS,
    STONE_BUTTON,
    STONE_HOE,
    STONE_PICKAXE,
    STONE_PRESSURE_PLATE,
    STONE_SHOVEL,
    STONE_SLAB,
    STONE_SWORD,
    STRAY_SPAWN_EGG,
    STRING,
    STRIPPED_ACACIA_LOG,
    STRIPPED_ACACIA_WOOD,
    STRIPPED_BIRCH_LOG,
    STRIPPED_BIRCH_WOOD,
    STRIPPED_DARK_OAK_LOG,
    STRIPPED_DARK_OAK_WOOD,
    STRIPPED_JUNGLE_LOG,
    STRIPPED_JUNGLE_WOOD,
    STRIPPED_OAK_LOG,
    STRIPPED_OAK_WOOD,
    STRIPPED_SPRUCE_LOG,
    STRIPPED_SPRUCE_WOOD,
    STRUCTURE_BLOCK,
    STRUCTURE_VOID,
    SUGAR,
    SUGAR_CANE,
    SUNFLOWER,
    TALL_GRASS,
    TERRACOTTA,
    TIPPED_ARROW,
    TNT,
    TNT_MINECART,
    TORCH,
    TOTEM_OF_UNDYING,
    TRAPPED_CHEST,
    TRIDENT,
    TRIPWIRE_HOOK,
    TROPICAL_FISH,
    TROPICAL_FISH_BUCKET,
    TROPICAL_FISH_SPAWN_EGG,
    TUBE_CORAL,
    TUBE_CORAL_BLOCK,
    TUBE_CORAL_FAN,
    TURTLE_EGG,
    TURTLE_HELMET,
    TURTLE_SPAWN_EGG,
    VEX_SPAWN_EGG,
    VILLAGER_SPAWN_EGG,
    VINDICATOR_SPAWN_EGG,
    VINE,
    WATER_BUCKET,
    WET_SPONGE,
    WHEAT,
    WHEAT_SEEDS,
    WHITE_BANNER,
    WHITE_BED,
    WHITE_CARPET,
    WHITE_CONCRETE,
    WHITE_CONCRETE_POWDER,
    WHITE_GLAZED_TERRACOTTA,
    WHITE_SHULKER_BOX,
    WHITE_STAINED_GLASS,
    WHITE_STAINED_GLASS_PANE,
    WHITE_TERRACOTTA,
    WHITE_TULIP,
    WHITE_WOOL,
    WITCH_SPAWN_EGG,
    WITHER_SKELETON_SKULL,
    WITHER_SKELETON_SPAWN_EGG,
    WOLF_SPAWN_EGG,
    WOODEN_AXE,
    WOODEN_HOE,
    WOODEN_PICKAXE,
    WOODEN_SHOVEL,
    WOODEN_SWORD,
    WRITABLE_BOOK,
    WRITTEN_BOOK,
    YELLOW_BANNER,
    YELLOW_BED,
    YELLOW_CARPET,
    YELLOW_CONCRETE,
    YELLOW_CONCRETE_POWDER,
    YELLOW_GLAZED_TERRACOTTA,
    YELLOW_SHULKER_BOX,
    YELLOW_STAINED_GLASS,
    YELLOW_STAINED_GLASS_PANE,
    YELLOW_TERRACOTTA,
    YELLOW_WOOL,
    ZOMBIE_HEAD,
    ZOMBIE_HORSE_SPAWN_EGG,
    ZOMBIE_PIGMAN_SPAWN_EGG,
    ZOMBIE_SPAWN_EGG,
    ZOMBIE_VILLAGER_SPAWN_EGG,

    ;

    /*
     -----------------------------------------------------
                    Instance
     -----------------------------------------------------
     */

    private BlockTypes blockType;
    private final String id;
    private final BaseItem defaultState;

    ItemTypes() {
        this(null);
    }

    ItemTypes(String id) {
        if (id == null) id = "minecraft:" + name().toLowerCase();
        // If it has no namespace, assume minecraft.
        else if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.id = id;
        this.defaultState = new BaseItemStack(this, 1);
    }

    private void setBlockType(BlockTypes type) {
        this.blockType = type;
    }

    @Override
    public BaseItem getDefaultState() {
        return defaultState;
    }

    public String getId() {
        return this.id;
    }

    @Deprecated
    public int getInternalId() {
        return ordinal();
    }

    /**
     * Gets the name of this item, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     */
    public String getName() {
        BundledItemData.ItemEntry entry = BundledItemData.getInstance().findById(this.id);
        if (entry == null) {
            return getId();
        } else {
            return entry.localizedName;
        }
    }


    /**
     * Gets whether this item type has a block representation.
     *
     * @return If it has a block
     */
    public boolean hasBlockType() {
        return getBlockType() != null;
    }

    /**
     * Gets the block representation of this item type, if it exists.
     *
     * @return The block representation
     */
    @Nullable
    public BlockTypes getBlockType() {
        return blockType;
    }

    @Override
    public String toString() {
        return getId();
    }

    /*
         -----------------------------------------------------
                        Static Initializer
         -----------------------------------------------------
         */
    private static final Map<String, ItemTypes> $REGISTRY = new HashMap<>();

    public static final ItemTypes[] values;

    static {
        try {
            Collection<String> items = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getItemRegistry().registerItems();
            if (!items.isEmpty()) { // No types found - use defaults
                for (String item : items) {
                    register(item);
                }
            }
            // Cache the values
            values = values();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static ItemTypes parse(String input) {
        input = input.toLowerCase();
        if (!Character.isAlphabetic(input.charAt(0))) {
            try {
                ItemTypes legacy = LegacyMapper.getInstance().getItemFromLegacy(input);
                if (legacy != null) return legacy;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (!input.split("\\[", 2)[0].contains(":")) input = "minecraft:" + input;
        ItemTypes result = $REGISTRY.get(input);
        if (result != null) return result;
        return null;
    }

    private static ItemTypes register(final String id) {
        // Get the enum name (remove namespace if minecraft:)
        int propStart = id.indexOf('[');
        String typeName = id.substring(0, propStart == -1 ? id.length() : propStart);
        String enumName = (typeName.startsWith("minecraft:") ? typeName.substring(10) : typeName).toUpperCase();
        // Check existing
        ItemTypes existing = valueOf(enumName.toUpperCase());
        if (existing != null) {
            // TODO additional registration
        } else {
            // Create it
            existing = ReflectionUtils.addEnum(ItemTypes.class, enumName, new Class[]{String.class}, new Object[]{id});
        }
        if (typeName.startsWith("minecraft:")) $REGISTRY.put(typeName.substring(10), existing);
        $REGISTRY.put(typeName, existing);
        return existing;
    }

    public static final @Nullable ItemTypes get(final String id) {
        return $REGISTRY.get(id);
    }

    public static final @Nullable ItemTypes get(BlockTypes type) {
        ItemTypes item = $REGISTRY.get(type.getId());
        if (item != null && item.getBlockType() == null) {
            item.setBlockType(type);
        }
        return item;
    }

    @Deprecated
    public static final ItemTypes get(final int ordinal) {
        return values[ordinal];
    }

    public static int size() {
        return values.length;
    }
}
