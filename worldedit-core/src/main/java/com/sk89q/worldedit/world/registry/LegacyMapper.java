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

package com.sk89q.worldedit.world.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.BlockFactory;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.Vector3;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.util.gson.VectorAdapter;
import com.sk89q.worldedit.util.io.ResourceLoader;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class LegacyMapper {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static LegacyMapper INSTANCE;
    private final ResourceLoader resourceLoader;

    //FAWE start
    private final Int2ObjectArrayMap<Integer> blockStateToLegacyId4Data = new Int2ObjectArrayMap<>();
    private final Int2ObjectArrayMap<Integer> extraId4DataToStateId = new Int2ObjectArrayMap<>();
    private final int[] blockArr = new int[4096];
    private final BiMap<Integer, ItemType> itemMap = HashBiMap.create();
    private final Map<String, String> blockEntries = new HashMap<>();
    //FAWE end
    private final Map<String, BlockState> stringToBlockMap = new HashMap<>();
    private final Multimap<BlockState, String> blockToStringMap = HashMultimap.create();

    /**
     * Create a new instance.
     */
    private LegacyMapper() {
        this.resourceLoader = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.CONFIGURATION).getResourceLoader();

        try {
            loadFromResource();
        } catch (Throwable e) {
            LOGGER.warn("Failed to load the built-in legacy id registry", e);
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
        URL url = resourceLoader.getResource(LegacyMapper.class, "legacy.json");
        if (url == null) {
            throw new IOException("Could not find legacy.json");
        }
        String data = Resources.toString(url, Charset.defaultCharset());
        LegacyDataFile dataFile = gson.fromJson(data, new TypeToken<LegacyDataFile>() {}.getType());

        DataFixer fixer = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataFixer();
        ParserContext parserContext = new ParserContext();
        parserContext.setPreferringWildcard(false);
        parserContext.setRestricted(false);
        parserContext.setTryLegacy(false); // This is legacy. Don't match itself.

        for (Map.Entry<String, String> blockEntry : dataFile.blocks.entrySet()) {
            String id = blockEntry.getKey();
            final String value = blockEntry.getValue();
            //FAWE start
            Integer combinedId = getCombinedId(blockEntry.getKey());
            blockEntries.put(id, value);
            //FAWE end

            BlockState state = null;
            //FAWE start
            try {
                state = BlockState.get(null, blockEntry.getValue());
                BlockType type = state.getBlockType();
                if (type.hasProperty(PropertyKey.WATERLOGGED)) {
                    state = state.with(PropertyKey.WATERLOGGED, false);
                }
            } catch (InputParseException f) {
                BlockFactory blockFactory = WorldEdit.getInstance().getBlockFactory();
            //FAWE end

                // if fixer is available, try using that first, as some old blocks that were renamed share names with new blocks
                if (fixer != null) {
                    try {
                        String newEntry = fixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, value, Constants.DATA_VERSION_MC_1_13_2);
                        state = blockFactory.parseFromInput(newEntry, parserContext).toImmutableState();
                    } catch (InputParseException ignored) {
                    }
                }

                // if it's still null, the fixer was unavailable or failed
                if (state == null) {
                    try {
                        state = blockFactory.parseFromInput(value, parserContext).toImmutableState();
                    } catch (InputParseException ignored) {
                    }
                }

                // if it's still null, both fixer and default failed
                if (state == null) {
                    LOGGER.debug("Unknown block: " + value);
                } else {
                    // it's not null so one of them succeeded, now use it
                    blockToStringMap.put(state, id);
                    stringToBlockMap.put(id, state);
                }
            }
            //FAWE start
            if (state != null) {
                blockArr[combinedId] = state.getInternalId();
                blockStateToLegacyId4Data.put(state.getInternalId(), (Integer) combinedId);
                blockStateToLegacyId4Data.putIfAbsent(state.getInternalBlockTypeId(), combinedId);
            }
        }
        for (int id = 0; id < 256; id++) {
            int combinedId = id << 4;
            int base = blockArr[combinedId];
            if (base != 0) {
                for (int data_ = 0; data_ < 16; data_++, combinedId++) {
                    if (blockArr[combinedId] == 0) {
                        blockArr[combinedId] = base;
                    }
                }
            }
        }
        //FAWE end

        for (Map.Entry<String, String> itemEntry : dataFile.items.entrySet()) {
            String id = itemEntry.getKey();
            String value = itemEntry.getValue();
            ItemType type = ItemTypes.get(value);
            if (type == null && fixer != null) {
                value = fixer.fixUp(DataFixer.FixTypes.ITEM_TYPE, value, Constants.DATA_VERSION_MC_1_13_2);
                type = ItemTypes.get(value);
            }
            if (type == null) {
                LOGGER.debug("Unknown item: " + value);
            } else {
                try {
                    itemMap.put(getCombinedId(id), type);
                } catch (Exception ignored) {
                }
            }
        }
    }

    //FAWE start
    private int getCombinedId(String input) {
        String[] split = input.split(":");
        return (Integer.parseInt(split[0]) << 4) + (split.length == 2 ? Integer.parseInt(split[1]) : 0);
    }

    @Nullable
    public ItemType getItemFromLegacy(int legacyId) {
        return itemMap.get(legacyId << 4);
    }

    public ItemType getItemFromLegacy(String input) {
        if (input.startsWith("minecraft:")) {
            input = input.substring(10);
        }
        return itemMap.get(getCombinedId(input));
    }
    //FAWE end

    //FAWE start - use internal ids
    public BlockState getBlockFromLegacy(String input) {
        if (input.startsWith("minecraft:")) {
            input = input.substring(10);
        }
        try {
            return BlockState.getFromInternalId(blockArr[getCombinedId(input)]);
        } catch (InputParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    //FAWE end

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

    //FAWE start
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
                if (internalId == 0) {
                    return null;
                }
                try {
                    return BlockState.getFromInternalId(internalId);
                } catch (InputParseException e) {
                    e.printStackTrace();
                }
            } catch (IndexOutOfBoundsException ignored) {
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
        if (result == null) {
            result = blockStateToLegacyId4Data.get(blockState.getInternalBlockTypeId());
        }
        return result;
    }

    @Nullable
    public Integer getLegacyCombined(BlockType type) {
        return blockStateToLegacyId4Data.get(type.getDefaultState().getInternalId());
    }
    //FAWE end

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

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private static class LegacyDataFile {
        private Map<String, String> blocks;
        private Map<String, String> items;
    }
}
