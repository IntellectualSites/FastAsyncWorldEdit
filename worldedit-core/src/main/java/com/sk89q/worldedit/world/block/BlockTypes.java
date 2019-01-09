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

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import it.unimi.dsi.fastutil.ints.IntCollections;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Stores a list of common Block String IDs.
 */
public enum BlockTypes implements BlockType {
    /*
     -----------------------------------------------------
        Replaced at runtime by the block registry
     -----------------------------------------------------
     */
    __RESERVED__,
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
    ATTACHED_MELON_STEM,
    ATTACHED_PUMPKIN_STEM,
    AZURE_BLUET,
    BARRIER,
    BEACON,
    BEDROCK,
    BEETROOTS,
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
    BLACK_WALL_BANNER,
    BLACK_WOOL,
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
    BLUE_WALL_BANNER,
    BLUE_WOOL,
    BONE_BLOCK,
    BOOKSHELF,
    BRAIN_CORAL,
    BRAIN_CORAL_BLOCK,
    BRAIN_CORAL_FAN,
    BRAIN_CORAL_WALL_FAN,
    BREWING_STAND,
    BRICK_SLAB,
    BRICK_STAIRS,
    BRICKS,
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
    BROWN_WALL_BANNER,
    BROWN_WOOL,
    BUBBLE_COLUMN,
    BUBBLE_CORAL,
    BUBBLE_CORAL_BLOCK,
    BUBBLE_CORAL_FAN,
    BUBBLE_CORAL_WALL_FAN,
    CACTUS,
    CAKE,
    CARROTS,
    CARVED_PUMPKIN,
    CAULDRON,
    CAVE_AIR,
    CHAIN_COMMAND_BLOCK,
    CHEST,
    CHIPPED_ANVIL,
    CHISELED_QUARTZ_BLOCK,
    CHISELED_RED_SANDSTONE,
    CHISELED_SANDSTONE,
    CHISELED_STONE_BRICKS,
    CHORUS_FLOWER,
    CHORUS_PLANT,
    CLAY,
    COAL_BLOCK,
    COAL_ORE,
    COARSE_DIRT,
    COBBLESTONE,
    COBBLESTONE_SLAB,
    COBBLESTONE_STAIRS,
    COBBLESTONE_WALL,
    COBWEB,
    COCOA,
    COMMAND_BLOCK,
    COMPARATOR,
    CONDUIT,
    CRACKED_STONE_BRICKS,
    CRAFTING_TABLE,
    CREEPER_HEAD,
    CREEPER_WALL_HEAD,
    CUT_RED_SANDSTONE,
    CUT_SANDSTONE,
    CYAN_BANNER,
    CYAN_BED,
    CYAN_CARPET,
    CYAN_CONCRETE,
    CYAN_CONCRETE_POWDER,
    CYAN_GLAZED_TERRACOTTA,
    CYAN_SHULKER_BOX,
    CYAN_STAINED_GLASS,
    CYAN_STAINED_GLASS_PANE,
    CYAN_TERRACOTTA,
    CYAN_WALL_BANNER,
    CYAN_WOOL,
    DAMAGED_ANVIL,
    DANDELION,
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
    DEAD_BRAIN_CORAL_WALL_FAN,
    DEAD_BUBBLE_CORAL_BLOCK,
    DEAD_BUBBLE_CORAL_FAN,
    DEAD_BUBBLE_CORAL_WALL_FAN,
    DEAD_BUSH,
    DEAD_FIRE_CORAL_BLOCK,
    DEAD_FIRE_CORAL_FAN,
    DEAD_FIRE_CORAL_WALL_FAN,
    DEAD_HORN_CORAL_BLOCK,
    DEAD_HORN_CORAL_FAN,
    DEAD_HORN_CORAL_WALL_FAN,
    DEAD_TUBE_CORAL_BLOCK,
    DEAD_TUBE_CORAL_FAN,
    DEAD_TUBE_CORAL_WALL_FAN,
    DETECTOR_RAIL,
    DIAMOND_BLOCK,
    DIAMOND_ORE,
    DIORITE,
    DIRT,
    DISPENSER,
    DRAGON_EGG,
    DRAGON_HEAD,
    DRAGON_WALL_HEAD,
    DRIED_KELP_BLOCK,
    DROPPER,
    EMERALD_BLOCK,
    EMERALD_ORE,
    ENCHANTING_TABLE,
    END_GATEWAY,
    END_PORTAL,
    END_PORTAL_FRAME,
    END_ROD,
    END_STONE,
    END_STONE_BRICKS,
    ENDER_CHEST,
    FARMLAND,
    FERN,
    FIRE,
    FIRE_CORAL,
    FIRE_CORAL_BLOCK,
    FIRE_CORAL_FAN,
    FIRE_CORAL_WALL_FAN,
    FLOWER_POT,
    FROSTED_ICE,
    FURNACE,
    GLASS,
    GLASS_PANE,
    GLOWSTONE,
    GOLD_BLOCK,
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
    GRAY_GLAZED_TERRACOTTA,
    GRAY_SHULKER_BOX,
    GRAY_STAINED_GLASS,
    GRAY_STAINED_GLASS_PANE,
    GRAY_TERRACOTTA,
    GRAY_WALL_BANNER,
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
    GREEN_WALL_BANNER,
    GREEN_WOOL,
    HAY_BLOCK,
    HEAVY_WEIGHTED_PRESSURE_PLATE,
    HOPPER,
    HORN_CORAL,
    HORN_CORAL_BLOCK,
    HORN_CORAL_FAN,
    HORN_CORAL_WALL_FAN,
    ICE,
    INFESTED_CHISELED_STONE_BRICKS,
    INFESTED_COBBLESTONE,
    INFESTED_CRACKED_STONE_BRICKS,
    INFESTED_MOSSY_STONE_BRICKS,
    INFESTED_STONE,
    INFESTED_STONE_BRICKS,
    IRON_BARS,
    IRON_BLOCK,
    IRON_DOOR,
    IRON_ORE,
    IRON_TRAPDOOR,
    JACK_O_LANTERN,
    JUKEBOX,
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
    KELP_PLANT,
    LADDER,
    LAPIS_BLOCK,
    LAPIS_ORE,
    LARGE_FERN,
    LAVA,
    LEVER,
    LIGHT_BLUE_BANNER,
    LIGHT_BLUE_BED,
    LIGHT_BLUE_CARPET,
    LIGHT_BLUE_CONCRETE,
    LIGHT_BLUE_CONCRETE_POWDER,
    LIGHT_BLUE_GLAZED_TERRACOTTA,
    LIGHT_BLUE_SHULKER_BOX,
    LIGHT_BLUE_STAINED_GLASS,
    LIGHT_BLUE_STAINED_GLASS_PANE,
    LIGHT_BLUE_TERRACOTTA,
    LIGHT_BLUE_WALL_BANNER,
    LIGHT_BLUE_WOOL,
    LIGHT_GRAY_BANNER,
    LIGHT_GRAY_BED,
    LIGHT_GRAY_CARPET,
    LIGHT_GRAY_CONCRETE,
    LIGHT_GRAY_CONCRETE_POWDER,
    LIGHT_GRAY_GLAZED_TERRACOTTA,
    LIGHT_GRAY_SHULKER_BOX,
    LIGHT_GRAY_STAINED_GLASS,
    LIGHT_GRAY_STAINED_GLASS_PANE,
    LIGHT_GRAY_TERRACOTTA,
    LIGHT_GRAY_WALL_BANNER,
    LIGHT_GRAY_WOOL,
    LIGHT_WEIGHTED_PRESSURE_PLATE,
    LILAC,
    LILY_PAD,
    LIME_BANNER,
    LIME_BED,
    LIME_CARPET,
    LIME_CONCRETE,
    LIME_CONCRETE_POWDER,
    LIME_GLAZED_TERRACOTTA,
    LIME_SHULKER_BOX,
    LIME_STAINED_GLASS,
    LIME_STAINED_GLASS_PANE,
    LIME_TERRACOTTA,
    LIME_WALL_BANNER,
    LIME_WOOL,
    MAGENTA_BANNER,
    MAGENTA_BED,
    MAGENTA_CARPET,
    MAGENTA_CONCRETE,
    MAGENTA_CONCRETE_POWDER,
    MAGENTA_GLAZED_TERRACOTTA,
    MAGENTA_SHULKER_BOX,
    MAGENTA_STAINED_GLASS,
    MAGENTA_STAINED_GLASS_PANE,
    MAGENTA_TERRACOTTA,
    MAGENTA_WALL_BANNER,
    MAGENTA_WOOL,
    MAGMA_BLOCK,
    MELON,
    MELON_STEM,
    MOSSY_COBBLESTONE,
    MOSSY_COBBLESTONE_WALL,
    MOSSY_STONE_BRICKS,
    MOVING_PISTON,
    MUSHROOM_STEM,
    MYCELIUM,
    NETHER_BRICK_FENCE,
    NETHER_BRICK_SLAB,
    NETHER_BRICK_STAIRS,
    NETHER_BRICKS,
    NETHER_PORTAL,
    NETHER_QUARTZ_ORE,
    NETHER_WART,
    NETHER_WART_BLOCK,
    NETHERRACK,
    NOTE_BLOCK,
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
    ORANGE_BANNER,
    ORANGE_BED,
    ORANGE_CARPET,
    ORANGE_CONCRETE,
    ORANGE_CONCRETE_POWDER,
    ORANGE_GLAZED_TERRACOTTA,
    ORANGE_SHULKER_BOX,
    ORANGE_STAINED_GLASS,
    ORANGE_STAINED_GLASS_PANE,
    ORANGE_TERRACOTTA,
    ORANGE_TULIP,
    ORANGE_WALL_BANNER,
    ORANGE_WOOL,
    OXEYE_DAISY,
    PACKED_ICE,
    PEONY,
    PETRIFIED_OAK_SLAB,
    PINK_BANNER,
    PINK_BED,
    PINK_CARPET,
    PINK_CONCRETE,
    PINK_CONCRETE_POWDER,
    PINK_GLAZED_TERRACOTTA,
    PINK_SHULKER_BOX,
    PINK_STAINED_GLASS,
    PINK_STAINED_GLASS_PANE,
    PINK_TERRACOTTA,
    PINK_TULIP,
    PINK_WALL_BANNER,
    PINK_WOOL,
    PISTON,
    PISTON_HEAD,
    PLAYER_HEAD,
    PLAYER_WALL_HEAD,
    PODZOL,
    POLISHED_ANDESITE,
    POLISHED_DIORITE,
    POLISHED_GRANITE,
    POPPY,
    POTATOES,
    POTTED_ACACIA_SAPLING,
    POTTED_ALLIUM,
    POTTED_AZURE_BLUET,
    POTTED_BIRCH_SAPLING,
    POTTED_BLUE_ORCHID,
    POTTED_BROWN_MUSHROOM,
    POTTED_CACTUS,
    POTTED_DANDELION,
    POTTED_DARK_OAK_SAPLING,
    POTTED_DEAD_BUSH,
    POTTED_FERN,
    POTTED_JUNGLE_SAPLING,
    POTTED_OAK_SAPLING,
    POTTED_ORANGE_TULIP,
    POTTED_OXEYE_DAISY,
    POTTED_PINK_TULIP,
    POTTED_POPPY,
    POTTED_RED_MUSHROOM,
    POTTED_RED_TULIP,
    POTTED_SPRUCE_SAPLING,
    POTTED_WHITE_TULIP,
    POWERED_RAIL,
    PRISMARINE,
    PRISMARINE_BRICK_SLAB,
    PRISMARINE_BRICK_STAIRS,
    PRISMARINE_BRICKS,
    PRISMARINE_SLAB,
    PRISMARINE_STAIRS,
    PUMPKIN,
    PUMPKIN_STEM,
    PURPLE_BANNER,
    PURPLE_BED,
    PURPLE_CARPET,
    PURPLE_CONCRETE,
    PURPLE_CONCRETE_POWDER,
    PURPLE_GLAZED_TERRACOTTA,
    PURPLE_SHULKER_BOX,
    PURPLE_STAINED_GLASS,
    PURPLE_STAINED_GLASS_PANE,
    PURPLE_TERRACOTTA,
    PURPLE_WALL_BANNER,
    PURPLE_WOOL,
    PURPUR_BLOCK,
    PURPUR_PILLAR,
    PURPUR_SLAB,
    PURPUR_STAIRS,
    QUARTZ_BLOCK,
    QUARTZ_PILLAR,
    QUARTZ_SLAB,
    QUARTZ_STAIRS,
    RAIL,
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
    RED_WALL_BANNER,
    RED_WOOL,
    REDSTONE_BLOCK,
    REDSTONE_LAMP,
    REDSTONE_ORE,
    REDSTONE_TORCH,
    REDSTONE_WALL_TORCH,
    REDSTONE_WIRE,
    REPEATER,
    REPEATING_COMMAND_BLOCK,
    ROSE_BUSH,
    SAND,
    SANDSTONE,
    SANDSTONE_SLAB,
    SANDSTONE_STAIRS,
    SEA_LANTERN,
    SEA_PICKLE,
    SEAGRASS,
    SHULKER_BOX,
    SIGN,
    SKELETON_SKULL,
    SKELETON_WALL_SKULL,
    SLIME_BLOCK,
    SMOOTH_QUARTZ,
    SMOOTH_RED_SANDSTONE,
    SMOOTH_SANDSTONE,
    SMOOTH_STONE,
    SNOW,
    SNOW_BLOCK,
    SOUL_SAND,
    SPAWNER,
    SPONGE,
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
    STICKY_PISTON,
    STONE,
    STONE_BRICK_SLAB,
    STONE_BRICK_STAIRS,
    STONE_BRICKS,
    STONE_BUTTON,
    STONE_PRESSURE_PLATE,
    STONE_SLAB,
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
    SUGAR_CANE,
    SUNFLOWER,
    TALL_GRASS,
    TALL_SEAGRASS,
    TERRACOTTA,
    TNT,
    TORCH,
    TRAPPED_CHEST,
    TRIPWIRE,
    TRIPWIRE_HOOK,
    TUBE_CORAL,
    TUBE_CORAL_BLOCK,
    TUBE_CORAL_FAN,
    TUBE_CORAL_WALL_FAN,
    TURTLE_EGG,
    VINE,
    VOID_AIR,
    WALL_SIGN,
    WALL_TORCH,
    WATER,
    WET_SPONGE,
    WHEAT,
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
    WHITE_WALL_BANNER,
    WHITE_WOOL,
    WITHER_SKELETON_SKULL,
    WITHER_SKELETON_WALL_SKULL,
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
    YELLOW_WALL_BANNER,
    YELLOW_WOOL,
    ZOMBIE_HEAD,
    ZOMBIE_WALL_HEAD,
    DEAD_BRAIN_CORAL,
    DEAD_BUBBLE_CORAL,
    DEAD_FIRE_CORAL,
    DEAD_HORN_CORAL,
    DEAD_TUBE_CORAL,

    ;

    /*
     -----------------------------------------------------
                    Instance
     -----------------------------------------------------
     */
    private final static class Settings {
        private final int internalId;
        private final ItemTypes itemType;
        private final BlockState defaultState;
        private final AbstractProperty[] propertiesMapArr;
        private final AbstractProperty[] propertiesArr;
        private final List<AbstractProperty> propertiesList;
        private final Map<String, AbstractProperty> propertiesMap;
        private final Set<AbstractProperty> propertiesSet;
        private final BlockMaterial blockMaterial;
        private final int permutations;
        private int[] stateOrdinals;

        Settings(BlockTypes type, String id, int internalId, List<BlockState> states) {
            this.internalId = internalId;
            String propertyString = null;
            int propI = id.indexOf('[');
            if (propI != -1) {
                propertyString = id.substring(propI + 1, id.length() - 1);
            }

            int maxInternalStateId = 0;
            Map<String, ? extends Property> properties = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getProperties(type);
            if (!properties.isEmpty()) {
                // Ensure the properties are registered
                int maxOrdinal = 0;
                for (String key : properties.keySet()) {
                    maxOrdinal = Math.max(PropertyKey.getOrCreate(key).ordinal(), maxOrdinal);
                }
                this.propertiesMapArr = new AbstractProperty[maxOrdinal + 1];
                int prop_arr_i = 0;
                this.propertiesArr = new AbstractProperty[properties.size()];
                HashMap<String, AbstractProperty> propMap = new HashMap<>();

                int bitOffset = 0;
                for (Map.Entry<String, ? extends Property> entry : properties.entrySet()) {
                    PropertyKey key = PropertyKey.getOrCreate(entry.getKey());
                    AbstractProperty property = ((AbstractProperty) entry.getValue()).withOffset(bitOffset);
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
            this.itemType = ItemTypes.get(type);

            if (!propertiesList.isEmpty()) {
                this.stateOrdinals = generateStateOrdinals(internalId, states.size(), maxInternalStateId, propertiesList);
                for (int propId = 0; propId < this.stateOrdinals.length; propId++) {
                    int ordinal = this.stateOrdinals[propId];
                    if (ordinal != -1) {
                        int stateId = internalId + (propId << BlockTypes.BIT_OFFSET);
                        states.add(new BlockStateImpl(type, stateId, ordinal));
                    }
                }
                int defaultPropId = parseProperties(propertyString, propertiesMap) >> BlockTypes.BIT_OFFSET;
                this.defaultState = states.get(this.stateOrdinals[defaultPropId]);
            } else {
                this.defaultState = new BlockStateImpl(type, internalId, states.size());
                states.add(this.defaultState);
            }
        }

        private int parseProperties(String properties, Map<String, AbstractProperty> propertyMap) {
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

    private final String id;
    private final Settings settings;

    BlockTypes() {
        if (name().indexOf(':') == -1) id = "minecraft:" + name().toLowerCase();
        else id = name().toLowerCase();
        settings = null;
    }

    private void init(String id, int internalId, List<BlockState> states) {
        try {
            if (getId() == null) {
                String name = (name().indexOf(':') == -1 ? "minecraft:" : "") + name().toLowerCase();
                ReflectionUtils.setFailsafeFieldValue(BlockTypes.class.getDeclaredField("id"), this, name);
            }
            Settings settings = new Settings(this, id, internalId, states);
            ReflectionUtils.setFailsafeFieldValue(BlockTypes.class.getDeclaredField("settings"), this, settings);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public BlockState withPropertyId(int propertyId) {
        if (settings.stateOrdinals == null) return settings.defaultState;
        return states[settings.stateOrdinals[propertyId]];
    }

    private static int[] generateStateOrdinals(int internalId, int ordinal, int maxStateId, List<AbstractProperty> props) {
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

    /**
     * Slow
     * @return collection of states
     */
    @Deprecated
    public List<BlockState> getAllStates() {
        if (settings.stateOrdinals == null) return Collections.singletonList(getDefaultState());
        return IntStream.of(settings.stateOrdinals).filter(i -> i != -1).mapToObj(i -> states[i]).collect(Collectors.toList());
    }

    @Deprecated
    public int getMaxStateId() {
        return settings.permutations;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return extent.setBlock(set, this.settings.defaultState);
    }

    public Mask toMask(Extent extent) {
        return new SingleBlockTypeMask(extent, this);
    }

    /**
     * Gets the ID of this block.
     *
     * @return The id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the name of this block, or the ID if the name cannot be found.
     *
     * @return The name, or ID
     */
    public String getName() {
        BundledBlockData.BlockEntry entry = BundledBlockData.getInstance().findById(this.id);
        if (entry == null) {
            return getId();
        } else {
            return entry.localizedName;
        }
    }

    @Deprecated
    public BlockState withStateId(int internalStateId) {
        return this.withPropertyId(internalStateId >> BlockTypes.BIT_OFFSET);
    }

    /**
     * Properties string in the form property1=foo,prop2=bar
     * @param properties
     * @return
     */
    public BlockState withProperties(String properties) {
        int id = getInternalId();
        for (String keyPair : properties.split(",")) {
            String[] split = keyPair.split("=");
            String name = split[0];
            String value = split[1];
            AbstractProperty btp = settings.propertiesMap.get(name);
            id = btp.modify(id, btp.getValueFor(value));
        }
        return withStateId(id);
    }

    /**
     * Gets the properties of this BlockType in a key->property mapping.
     *
     * @return The properties map
     */
    @Deprecated
    public Map<String, ? extends Property> getPropertyMap() {
        return this.settings.propertiesMap;
    }

    /**
     * Gets the properties of this BlockType.
     *
     * @return the properties
     */
    @Deprecated
    public List<? extends Property> getProperties() {
        return this.settings.propertiesList;
    }

    @Deprecated
    public Set<? extends Property> getPropertiesSet() {
        return this.settings.propertiesSet;
    }

    /**
     * Gets a property by name.
     *
     * @param name The name
     * @return The property
     */
    @Deprecated
    public <V> Property<V> getProperty(String name) {
        return this.settings.propertiesMap.get(name);
    }

    public boolean hasProperty(PropertyKey key) {
        int ordinal = key.ordinal();
        return this.settings.propertiesMapArr.length > ordinal ? this.settings.propertiesMapArr[ordinal] != null : false;
    }

    public <V> Property<V> getProperty(PropertyKey key) {
        try {
            return this.settings.propertiesMapArr[key.ordinal()];
        } catch (IndexOutOfBoundsException ignore) {
            return null;
        }
    }

    /**
     * Gets the default state of this block type.
     *
     * @return The default state
     */
    public BlockState getDefaultState() {
        return this.settings.defaultState;
    }

    /**
     * Gets whether this block type has an item representation.
     *
     * @return If it has an item
     */
    public boolean hasItemType() {
        return getItemType() != null;
    }

    /**
     * Gets the item representation of this block type, if it exists.
     *
     * @return The item representation
     */
    @Nullable
    public ItemType getItemType() {
        return settings.itemType;
    }

    /**
     * Get the material for this BlockType.
     *
     * @return The material
     */
    public BlockMaterial getMaterial() {
        return this.settings.blockMaterial;
    }

    /**
     * The internal index of this type.
     *
     * This number is not necessarily consistent across restarts.
     *
     * @return internal id
     */
    public int getInternalId() {
        return this.settings.internalId;
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

    @Deprecated public static final int BIT_OFFSET; // Used internally
    @Deprecated public static final int BIT_MASK; // Used internally

    private static final Map<String, BlockTypes> $REGISTRY = new HashMap<>();
    private static int $LENGTH;
    private static int $STATE_INDEX;

    public static final BlockTypes[] values;
    public static final BlockState[] states;

    private static final Set<String> $NAMESPACES = new LinkedHashSet<String>();

    static {
        try {
            Collection<String> blocks = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().registerBlocks();
            Map<String, String> blockMap = blocks.stream().collect(Collectors.toMap(item -> item.charAt(item.length() - 1) == ']' ? item.substring(0, item.indexOf('[')) : item, item -> item));

            BlockTypes[] oldValues = BlockTypes.values();
            $LENGTH = oldValues.length;
            int size = blockMap.size();
            for (BlockTypes type : oldValues) {
                if (!blockMap.containsKey(type.getId())) {
                    type.init(type.getId(), 0, new ArrayList<>());
                    if (type != __RESERVED__) Fawe.debug("Invalid block registered " + type.getId());
                    size++;
                }
                if (type != __RESERVED__) {
                    $REGISTRY.put(type.name().toLowerCase(), type);
                }
            }

            BIT_OFFSET = MathMan.log2nlz(size);
            BIT_MASK = ((1 << BIT_OFFSET) - 1);

            LinkedHashSet<BlockTypes> newValues = new LinkedHashSet<>(Arrays.asList(oldValues));
            ArrayList<BlockState> stateList = new ArrayList<>();
            for (String block : blocks) {
                BlockTypes registered = register(block, stateList);
                if (!newValues.contains(registered)) newValues.add(registered);
            }
            // Cache the values
            values = newValues.toArray(new BlockTypes[newValues.size()]);
            states = stateList.toArray(new BlockState[stateList.size()]);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static BlockTypes parse(final String type) throws InputParseException {
        final String inputLower = type.toLowerCase();
        String input = inputLower;

        if (!input.split("\\[", 2)[0].contains(":")) input = "minecraft:" + input;
        BlockTypes result = $REGISTRY.get(input);
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

    private static BlockTypes register(final String id, List<BlockState> states) {
        // Get the enum name (remove namespace if minecraft:)
        int propStart = id.indexOf('[');
        String typeName = id.substring(0, propStart == -1 ? id.length() : propStart);
        String enumName = (typeName.startsWith("minecraft:") ? typeName.substring(10) : typeName).toUpperCase();
        // Check existing
        BlockTypes existing = null;
        try {
            existing = valueOf(enumName.toUpperCase());
        } catch (IllegalArgumentException ignore) {}
        if (existing == null) {
            Fawe.debug("Registering block " + enumName);
            existing = ReflectionUtils.addEnum(BlockTypes.class, enumName);
        }
        int internalId = existing.ordinal();
        if (internalId == 0 && existing != __RESERVED__) {
            internalId = $LENGTH++;
        }
        existing.init(id, internalId, states);
        // register states
        if (typeName.startsWith("minecraft:")) $REGISTRY.put(typeName.substring(10), existing);
        $REGISTRY.put(typeName, existing);
        String nameSpace = typeName.substring(0, typeName.indexOf(':'));
        $NAMESPACES.add(nameSpace);
        return existing;
    }

    public static Set<String> getNameSpaces() {
        return $NAMESPACES;
    }

    public static final @Nullable BlockTypes get(final String id) {
        return $REGISTRY.get(id);
    }

    public static final @Nullable BlockTypes get(final CharSequence id) {
        return $REGISTRY.get(id);
    }

    @Deprecated
    public static final BlockTypes get(final int ordinal) {
        return values[ordinal];
    }

    @Deprecated
    public static final BlockTypes getFromStateId(final int internalStateId) {
        return values[internalStateId & BIT_MASK];
    }

    @Deprecated
    public static final BlockTypes getFromStateOrdinal(final int internalStateOrdinal) {
        return states[internalStateOrdinal].getBlockType();
    }

    public static int size() {
        return values.length;
    }
}
