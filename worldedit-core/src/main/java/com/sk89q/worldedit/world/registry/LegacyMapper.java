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

package com.sk89q.worldedit.world.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.util.gson.VectorAdapter;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

public class LegacyMapper {

    private static final Logger log = Logger.getLogger(LegacyMapper.class.getCanonicalName());
    private static LegacyMapper INSTANCE;

    private final Int2ObjectArrayMap<Integer> blockStateToLegacyId4Data = new Int2ObjectArrayMap<>();
    private final Int2ObjectArrayMap<Integer> extraId4DataToStateId = new Int2ObjectArrayMap<>();
    private final int[] blockArr = new int[4096];
    private final BiMap<Integer, ItemTypes> itemMap = HashBiMap.create();

    /**
     * Create a new instance.
     */
    private LegacyMapper() {
        try {
            loadFromResource();
        } catch (Throwable e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Failed to load the built-in legacy id registry", e);
        }
    }

    /**
     * Attempt to load the data from file.
     *
     * @throws IOException thrown on I/O error
     */
    private void loadFromResource() throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Vector.class, new VectorAdapter());
        Gson gson = gsonBuilder.disableHtmlEscaping().create();
        URL url = LegacyMapper.class.getResource("legacy.json");
        if (url == null) {
            throw new IOException("Could not find legacy.json");
        }
        String source = Resources.toString(url, Charset.defaultCharset());
        LegacyDataFile dataFile = gson.fromJson(source, new TypeToken<LegacyDataFile>() {}.getType());

        ParserContext parserContext = new ParserContext();
        parserContext.setPreferringWildcard(false);
        parserContext.setRestricted(false);
        parserContext.setTryLegacy(false); // This is legacy. Don't match itself.

        for (Map.Entry<String, String> blockEntry : dataFile.blocks.entrySet()) {
            try {
                BlockStateHolder blockState = BlockState.get(null, blockEntry.getValue());
                BlockTypes type = blockState.getBlockType();
                if (type.hasProperty(PropertyKey.WATERLOGGED)) {
                    blockState = blockState.with(PropertyKey.WATERLOGGED, false);
                }
                int combinedId = getCombinedId(blockEntry.getKey());
                blockArr[combinedId] = blockState.getInternalId();

                blockStateToLegacyId4Data.put(blockState.getInternalId(), (Integer) combinedId);
                blockStateToLegacyId4Data.putIfAbsent(blockState.getInternalBlockTypeId(), combinedId);
            } catch (Exception e) {
                log.fine("Unknown block: " + blockEntry.getValue());
            }
        }
        for (int id = 0; id < 256; id++) {
            int combinedId = id << 4;
            int base = blockArr[combinedId];
            if (base != 0) {
                for (int data = 0; data < 16; data++, combinedId++) {
                    if (blockArr[combinedId] == 0) blockArr[combinedId] = base;
                }
            }
        }

        for (Map.Entry<String, String> itemEntry : dataFile.items.entrySet()) {
            try {
                itemMap.put(getCombinedId(itemEntry.getKey()), ItemTypes.get(itemEntry.getValue()));
            } catch (Exception e) {
                log.fine("Unknown item: " + itemEntry.getValue());
            }
        }
    }

    private int getCombinedId(String input) {
        String[] split = input.split(":");
        return (Integer.parseInt(split[0]) << 4) + (split.length == 2 ? Integer.parseInt(split[1]) : 0);
    }

    @Nullable
    public ItemTypes getItemFromLegacy(int legacyId) {
        return itemMap.get(legacyId << 4);
    }

    public ItemTypes getItemFromLegacy(String input) {
        if (input.startsWith("minecraft:")) input = input.substring(10);
        return itemMap.get(getCombinedId(input));
    }

    public BlockState getBlockFromLegacy(String input) {
        if (input.startsWith("minecraft:")) input = input.substring(10);
        return BlockState.getFromInternalId(blockArr[getCombinedId(input)]);
    }

    @Nullable
    public ItemTypes getItemFromLegacy(int legacyId, int data) {
        return itemMap.get((legacyId << 4) + data);
    }

    @Nullable
    public Integer getLegacyCombined(ItemType itemType) {
        return itemMap.inverse().get(itemType);
    }

    @Nullable
    public int[] getLegacyFromItem(ItemType itemType) {
        Integer combinedId = getLegacyCombined(itemType);
        return combinedId == null ? null : new int[] { combinedId >> 4, combinedId & 0xF };
    }

    @Nullable
    public BlockState getBlockFromLegacy(int legacyId) {
        return getBlock(legacyId << 4);
    }

    @Nullable
    public BlockState getBlockFromLegacyCombinedId(int combinedId) {
        return getBlock(combinedId);
    }

    @Nullable
    public BlockState getBlockFromLegacy(int legacyId, int data) {
        return getBlock((legacyId << 4) + data);
    }

    private BlockState getBlock(int combinedId) {
        if (combinedId < blockArr.length) {
            try {
                int internalId = blockArr[combinedId];
                if (internalId == 0) return null;
                return BlockState.getFromInternalId(internalId);
            } catch (IndexOutOfBoundsException ignore) {
                return null;
            }
        }
        Integer extra = extraId4DataToStateId.get(combinedId);
        if (extra == null) {
            extra = extraId4DataToStateId.get(combinedId & 0xFF0);
        }
        if (extra != null) {
            return BlockState.getFromInternalId(extra);
        }
        return null;
    }

    public void register(int id, int data, BlockStateHolder state) {
        int combinedId = ((id << 4) + data);
        extraId4DataToStateId.put((int) combinedId, (Integer) state.getInternalId());
        blockStateToLegacyId4Data.putIfAbsent(state.getInternalId(), combinedId);
    }

    @Nullable
    public Integer getLegacyCombined(BlockState blockState) {
        Integer result = blockStateToLegacyId4Data.get(blockState.getInternalId());
        if (result == null) result = blockStateToLegacyId4Data.get(blockState.getInternalBlockTypeId());
        return result;
    }

    @Nullable
    public Integer getLegacyCombined(BlockType type) {
        return blockStateToLegacyId4Data.get(type.getDefaultState().getInternalId());
    }

    @Deprecated
    public int[] getLegacyFromBlock(BlockState blockState) {
        Integer combinedId = getLegacyCombined(blockState);
        return combinedId == null ? null : new int[] { combinedId >> 4, combinedId & 0xF };
    }

    public static LegacyMapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LegacyMapper();
        }
        return INSTANCE;
    }

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
    private static class LegacyDataFile {
        private Map<String, String> blocks;
        private Map<String, String> items;
    }
}
