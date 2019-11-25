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

package com.sk89q.worldedit.world.biome;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Stores a list of common Biome String IDs.
 */
public final class BiomeTypes {

    @Nullable public static final BiomeType BADLANDS = get("minecraft:badlands");
    @Nullable public static final BiomeType BADLANDS_PLATEAU = get("minecraft:badlands_plateau");
    @Nullable public static final BiomeType BAMBOO_JUNGLE = get("minecraft:bamboo_jungle");
    @Nullable public static final BiomeType BAMBOO_JUNGLE_HILLS = get("minecraft:bamboo_jungle_hills");
    @Nullable public static final BiomeType BEACH = get("minecraft:beach");
    @Nullable public static final BiomeType BIRCH_FOREST = get("minecraft:birch_forest");
    @Nullable public static final BiomeType BIRCH_FOREST_HILLS = get("minecraft:birch_forest_hills");
    @Nullable public static final BiomeType COLD_OCEAN = get("minecraft:cold_ocean");
    @Nullable public static final BiomeType DARK_FOREST = get("minecraft:dark_forest");
    @Nullable public static final BiomeType DARK_FOREST_HILLS = get("minecraft:dark_forest_hills");
    @Nullable public static final BiomeType DEEP_COLD_OCEAN = get("minecraft:deep_cold_ocean");
    @Nullable public static final BiomeType DEEP_FROZEN_OCEAN = get("minecraft:deep_frozen_ocean");
    @Nullable public static final BiomeType DEEP_LUKEWARM_OCEAN = get("minecraft:deep_lukewarm_ocean");
    @Nullable public static final BiomeType DEEP_OCEAN = get("minecraft:deep_ocean");
    @Nullable public static final BiomeType DEEP_WARM_OCEAN = get("minecraft:deep_warm_ocean");
    @Nullable public static final BiomeType DESERT = get("minecraft:desert");
    @Nullable public static final BiomeType DESERT_HILLS = get("minecraft:desert_hills");
    @Nullable public static final BiomeType DESERT_LAKES = get("minecraft:desert_lakes");
    @Nullable public static final BiomeType END_BARRENS = get("minecraft:end_barrens");
    @Nullable public static final BiomeType END_HIGHLANDS = get("minecraft:end_highlands");
    @Nullable public static final BiomeType END_MIDLANDS = get("minecraft:end_midlands");
    @Nullable public static final BiomeType ERODED_BADLANDS = get("minecraft:eroded_badlands");
    @Nullable public static final BiomeType FLOWER_FOREST = get("minecraft:flower_forest");
    @Nullable public static final BiomeType FOREST = get("minecraft:forest");
    @Nullable public static final BiomeType FROZEN_OCEAN = get("minecraft:frozen_ocean");
    @Nullable public static final BiomeType FROZEN_RIVER = get("minecraft:frozen_river");
    @Nullable public static final BiomeType GIANT_SPRUCE_TAIGA = get("minecraft:giant_spruce_taiga");
    @Nullable public static final BiomeType GIANT_SPRUCE_TAIGA_HILLS = get("minecraft:giant_spruce_taiga_hills");
    @Nullable public static final BiomeType GIANT_TREE_TAIGA = get("minecraft:giant_tree_taiga");
    @Nullable public static final BiomeType GIANT_TREE_TAIGA_HILLS = get("minecraft:giant_tree_taiga_hills");
    @Nullable public static final BiomeType GRAVELLY_MOUNTAINS = get("minecraft:gravelly_mountains");
    @Nullable public static final BiomeType ICE_SPIKES = get("minecraft:ice_spikes");
    @Nullable public static final BiomeType JUNGLE = get("minecraft:jungle");
    @Nullable public static final BiomeType JUNGLE_EDGE = get("minecraft:jungle_edge");
    @Nullable public static final BiomeType JUNGLE_HILLS = get("minecraft:jungle_hills");
    @Nullable public static final BiomeType LUKEWARM_OCEAN = get("minecraft:lukewarm_ocean");
    @Nullable public static final BiomeType MODIFIED_BADLANDS_PLATEAU = get("minecraft:modified_badlands_plateau");
    @Nullable public static final BiomeType MODIFIED_GRAVELLY_MOUNTAINS = get("minecraft:modified_gravelly_mountains");
    @Nullable public static final BiomeType MODIFIED_JUNGLE = get("minecraft:modified_jungle");
    @Nullable public static final BiomeType MODIFIED_JUNGLE_EDGE = get("minecraft:modified_jungle_edge");
    @Nullable public static final BiomeType MODIFIED_WOODED_BADLANDS_PLATEAU = get("minecraft:modified_wooded_badlands_plateau");
    @Nullable public static final BiomeType MOUNTAIN_EDGE = get("minecraft:mountain_edge");
    @Nullable public static final BiomeType MOUNTAINS = get("minecraft:mountains");
    @Nullable public static final BiomeType MUSHROOM_FIELD_SHORE = get("minecraft:mushroom_field_shore");
    @Nullable public static final BiomeType MUSHROOM_FIELDS = get("minecraft:mushroom_fields");
    @Nullable public static final BiomeType NETHER = get("minecraft:nether");
    @Nullable public static final BiomeType OCEAN = get("minecraft:ocean");
    @Nullable public static final BiomeType PLAINS = get("minecraft:plains");
    @Nullable public static final BiomeType RIVER = get("minecraft:river");
    @Nullable public static final BiomeType SAVANNA = get("minecraft:savanna");
    @Nullable public static final BiomeType SAVANNA_PLATEAU = get("minecraft:savanna_plateau");
    @Nullable public static final BiomeType SHATTERED_SAVANNA = get("minecraft:shattered_savanna");
    @Nullable public static final BiomeType SHATTERED_SAVANNA_PLATEAU = get("minecraft:shattered_savanna_plateau");
    @Nullable public static final BiomeType SMALL_END_ISLANDS = get("minecraft:small_end_islands");
    @Nullable public static final BiomeType SNOWY_BEACH = get("minecraft:snowy_beach");
    @Nullable public static final BiomeType SNOWY_MOUNTAINS = get("minecraft:snowy_mountains");
    @Nullable public static final BiomeType SNOWY_TAIGA = get("minecraft:snowy_taiga");
    @Nullable public static final BiomeType SNOWY_TAIGA_HILLS = get("minecraft:snowy_taiga_hills");
    @Nullable public static final BiomeType SNOWY_TAIGA_MOUNTAINS = get("minecraft:snowy_taiga_mountains");
    @Nullable public static final BiomeType SNOWY_TUNDRA = get("minecraft:snowy_tundra");
    @Nullable public static final BiomeType STONE_SHORE = get("minecraft:stone_shore");
    @Nullable public static final BiomeType SUNFLOWER_PLAINS = get("minecraft:sunflower_plains");
    @Nullable public static final BiomeType SWAMP = get("minecraft:swamp");
    @Nullable public static final BiomeType SWAMP_HILLS = get("minecraft:swamp_hills");
    @Nullable public static final BiomeType TAIGA = get("minecraft:taiga");
    @Nullable public static final BiomeType TAIGA_HILLS = get("minecraft:taiga_hills");
    @Nullable public static final BiomeType TAIGA_MOUNTAINS = get("minecraft:taiga_mountains");
    @Nullable public static final BiomeType TALL_BIRCH_FOREST = get("minecraft:tall_birch_forest");
    @Nullable public static final BiomeType TALL_BIRCH_HILLS = get("minecraft:tall_birch_hills");
    @Nullable public static final BiomeType THE_END = get("minecraft:the_end");
    @Nullable public static final BiomeType THE_VOID = get("minecraft:the_void");
    @Nullable public static final BiomeType WARM_OCEAN = get("minecraft:warm_ocean");
    @Nullable public static final BiomeType WOODED_BADLANDS_PLATEAU = get("minecraft:wooded_badlands_plateau");
    @Nullable public static final BiomeType WOODED_HILLS = get("minecraft:wooded_hills");
    @Nullable public static final BiomeType WOODED_MOUNTAINS = get("minecraft:wooded_mountains");

    private BiomeTypes() {
    }

    private static BiomeType register(final String id) {
        return register(new BiomeType(id));
    }

    public static BiomeType register(final BiomeType biome) {
        return BiomeType.REGISTRY.register(biome.getId(), biome);
    }

    public static BiomeType getLegacy(int legacyId) {
        for (BiomeType type : values()) {
            if (type.getLegacyId() == legacyId) {
                return type;
            }
        }
        return null;
    }

    public static BiomeType get(int internalId) {
        return BiomeType.REGISTRY.getByInternalId(internalId);
    }

    public static @Nullable BiomeType get(final String id) {
        return BiomeType.REGISTRY.get(id);
    }

    public static Collection<BiomeType> values() {
        return BiomeType.REGISTRY.values();
    }

    public static int getMaxId() {
        int maxBiomeId = 0;
        for (BiomeType type : BiomeType.REGISTRY.values()) {
            if (type.getInternalId() > maxBiomeId) {
                maxBiomeId = type.getInternalId();
            }
        }
        return maxBiomeId;
    }

    static {
        OCEAN.setLegacyId(0);
        PLAINS.setLegacyId(1);
        DESERT.setLegacyId(2);
        MOUNTAINS.setLegacyId(3);
        FOREST.setLegacyId(4);
        TAIGA.setLegacyId(5);
        SWAMP.setLegacyId(6);
        RIVER.setLegacyId(7);
        NETHER.setLegacyId(8);
        THE_END.setLegacyId(9);
        FROZEN_OCEAN.setLegacyId(10);
        FROZEN_RIVER.setLegacyId(11);
        SNOWY_TUNDRA.setLegacyId(12);
        SNOWY_MOUNTAINS.setLegacyId(13);
        MUSHROOM_FIELDS.setLegacyId(14);
        MUSHROOM_FIELD_SHORE.setLegacyId(15);
        BEACH.setLegacyId(16);
        DESERT_HILLS.setLegacyId(17);
        WOODED_HILLS.setLegacyId(18);
        TAIGA_HILLS.setLegacyId(19);
        MOUNTAIN_EDGE.setLegacyId(20);
        JUNGLE.setLegacyId(21);
        JUNGLE_HILLS.setLegacyId(22);
        JUNGLE_EDGE.setLegacyId(23);
        DEEP_OCEAN.setLegacyId(24);
        STONE_SHORE.setLegacyId(25);
        SNOWY_BEACH.setLegacyId(26);
        BIRCH_FOREST.setLegacyId(27);
        BIRCH_FOREST_HILLS.setLegacyId(28);
        DARK_FOREST.setLegacyId(29);
        SNOWY_TAIGA.setLegacyId(30);
        SNOWY_TAIGA_HILLS.setLegacyId(31);
        GIANT_TREE_TAIGA.setLegacyId(32);
        GIANT_TREE_TAIGA_HILLS.setLegacyId(33);
        WOODED_MOUNTAINS.setLegacyId(34);
        SAVANNA.setLegacyId(35);
        SAVANNA_PLATEAU.setLegacyId(36);
        BADLANDS.setLegacyId(37);
        WOODED_BADLANDS_PLATEAU.setLegacyId(38);
        BADLANDS_PLATEAU.setLegacyId(39);
        SMALL_END_ISLANDS.setLegacyId(40);
        END_MIDLANDS.setLegacyId(41);
        END_HIGHLANDS.setLegacyId(42);
        END_BARRENS.setLegacyId(43);
        WARM_OCEAN.setLegacyId(44);
        LUKEWARM_OCEAN.setLegacyId(45);
        COLD_OCEAN.setLegacyId(46);
        DEEP_WARM_OCEAN.setLegacyId(47);
        DEEP_LUKEWARM_OCEAN.setLegacyId(48);
        DEEP_COLD_OCEAN.setLegacyId(49);
        DEEP_FROZEN_OCEAN.setLegacyId(50);
        THE_VOID.setLegacyId(127);
        SUNFLOWER_PLAINS.setLegacyId(129);
        DESERT_LAKES.setLegacyId(130);
        GRAVELLY_MOUNTAINS.setLegacyId(131);
        FLOWER_FOREST.setLegacyId(132);
        TAIGA_MOUNTAINS.setLegacyId(133);
        SWAMP_HILLS.setLegacyId(134);
        ICE_SPIKES.setLegacyId(140);
        MODIFIED_JUNGLE.setLegacyId(149);
        MODIFIED_JUNGLE_EDGE.setLegacyId(151);
        TALL_BIRCH_FOREST.setLegacyId(155);
        TALL_BIRCH_HILLS.setLegacyId(156);
        DARK_FOREST_HILLS.setLegacyId(157);
        SNOWY_TAIGA_MOUNTAINS.setLegacyId(158);
        GIANT_SPRUCE_TAIGA.setLegacyId(160);
        GIANT_SPRUCE_TAIGA_HILLS.setLegacyId(161);
        MODIFIED_GRAVELLY_MOUNTAINS.setLegacyId(162);
        SHATTERED_SAVANNA.setLegacyId(163);
        SHATTERED_SAVANNA_PLATEAU.setLegacyId(164);
        ERODED_BADLANDS.setLegacyId(165);
        MODIFIED_WOODED_BADLANDS_PLATEAU.setLegacyId(166);
        MODIFIED_BADLANDS_PLATEAU.setLegacyId(167);
//        BAMBOO_JUNGLE.setLegacyId(168);
//        BAMBOO_JUNGLE_HILLS.setLegacyId(169);
    }
}
