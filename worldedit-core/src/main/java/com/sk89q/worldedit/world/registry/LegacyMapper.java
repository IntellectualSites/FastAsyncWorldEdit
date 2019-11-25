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
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.gson.VectorAdapter;
import com.sk89q.worldedit.util.io.ResourceLoader;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public final class LegacyMapper {

    private static final Logger log = LoggerFactory.getLogger(LegacyMapper.class);
    private static LegacyMapper INSTANCE = new LegacyMapper();

    private final Int2ObjectArrayMap<Integer> blockStateToLegacyId4Data = new Int2ObjectArrayMap<>();
    private final Int2ObjectArrayMap<Integer> extraId4DataToStateId = new Int2ObjectArrayMap<>();
    private final int[] blockArr = new int[4096];
    private final BiMap<Integer, ItemType> itemMap = HashBiMap.create();

    /**
     * Create a new instance.
     */
    private LegacyMapper() {
        try {
            loadFromResource();
        } catch (Throwable e) {
            log.warn("Failed to load the built-in legacy id registry", e);
        }
    }

    /**
     * Attempt to load the data from file.
     *
     * @throws IOException thrown on I/O error
     */
    private void loadFromResource() throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Vector3.class, new VectorAdapter());
        Gson gson = gsonBuilder.disableHtmlEscaping().create();
        URL url = ResourceLoader.getResource(LegacyMapper.class, "legacy.json");
        if (url == null) {
            throw new IOException("Could not find legacy.json");
        }
        String source = Resources.toString(url, Charset.defaultCharset());
        LegacyDataFile dataFile = gson.fromJson(source, new TypeToken<LegacyDataFile>() {}.getType());

        DataFixer fixer = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataFixer();
        ParserContext parserContext = new ParserContext();
        parserContext.setPreferringWildcard(false);
        parserContext.setRestricted(false);
        parserContext.setTryLegacy(false); // This is legacy. Don't match itself.

        for (Map.Entry<String, String> blockEntry : dataFile.blocks.entrySet()) {
            try {
                BlockState blockState = BlockState.get(null, blockEntry.getValue());
                BlockType type = blockState.getBlockType();
                if (type.hasProperty(PropertyKey.WATERLOGGED)) {
                    blockState = blockState.with(PropertyKey.WATERLOGGED, false);
                }
                Integer combinedId = getCombinedId(blockEntry.getKey());
                blockArr[combinedId] = blockState.getInternalId();

                blockStateToLegacyId4Data.put(blockState.getInternalId(), (Integer) combinedId);
                blockStateToLegacyId4Data.putIfAbsent(blockState.getInternalBlockTypeId(), combinedId);
            } catch (InputParseException e) {
                log.warn("Unknown block: " + blockEntry.getValue());
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
                log.warn("Unknown item: " + itemEntry.getValue());
            }
        }
    }

    private int getCombinedId(String input) {
        String[] split = input.split(":");
        return (Integer.parseInt(split[0]) << 4) + (split.length == 2 ? Integer.parseInt(split[1]) : 0);
    }

    @Nullable
    public ItemType getItemFromLegacy(int legacyId) {
        return itemMap.get(legacyId << 4);
    }

    public ItemType getItemFromLegacy(String input) {
        if (input.startsWith("minecraft:")) input = input.substring(10);
        return itemMap.get(getCombinedId(input));
    }

    public BlockState getBlockFromLegacy(String input) {
        if (input.startsWith("minecraft:")) input = input.substring(10);
        try {
            return BlockState.getFromInternalId(blockArr[getCombinedId(input)]);
        } catch (InputParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public ItemType getItemFromLegacy(int legacyId, int data) {
        return itemMap.get((legacyId << 4) + data);
    }

    @Nullable
    public Integer getLegacyCombined(ItemType itemType) {
        return itemMap.inverse().get(itemType);
    }

    @Nullable
    public int[] getLegacyFromItem(ItemType itemType) {
        Integer combinedId = getLegacyCombined(itemType);
        if (combinedId == null) {
            return null;
        } else {
            return new int[]{combinedId >> 4, combinedId & 0xF};
        }
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
                try {
                    return BlockState.getFromInternalId(internalId);
                } catch (InputParseException e) {
                    e.printStackTrace();
                }
            } catch (IndexOutOfBoundsException ignore) {
                return null;
            }
        }
        Integer extra = extraId4DataToStateId.get(combinedId);
        if (extra == null) {
            extra = extraId4DataToStateId.get(combinedId & 0xFF0);
        }
        if (extra != null) {
            try {
                return BlockState.getFromInternalId(extra);
            } catch (InputParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void register(int id, int data, BlockStateHolder state) {
        Integer combinedId = ((id << 4) + data);
        extraId4DataToStateId.put(combinedId, (Integer) state.getInternalId());
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

    public final static LegacyMapper getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private static class LegacyDataFile {
        private Map<String, String> blocks;
        private Map<String, String> items;
    }
}
