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

package com.sk89q.worldedit.world.chunk;

import com.fastasyncworldedit.core.util.NbtUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.BinaryTagTypes;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.util.nbt.ListBinaryTag;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.storage.InvalidFormatException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The chunk format for Minecraft 1.18 and newer
 */
public class AnvilChunk18 implements Chunk {

    //FAWE start - CBT
    private final CompoundBinaryTag rootTag;
    //FAWE end
    private final Int2ObjectOpenHashMap<BlockState[]> blocks;
    //FAWE start - entity and biome restore
    private final int sectionCount;
    private final Supplier<CompoundBinaryTag> entityTagSupplier;
    private Int2ObjectOpenHashMap<BiomeType[]> biomes = null;
    private List<BaseEntity> entities;
    private Map<BlockVector3, CompoundBinaryTag> tileEntities;
    //FAWE end


    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag the tag to read
     * @throws DataException on a data error
     * @deprecated Use {@link AnvilChunk18#AnvilChunk18(CompoundBinaryTag, Supplier)}
     */
    @Deprecated
    public AnvilChunk18(CompoundTag tag) throws DataException {
        //FAWE start - CBT
        this(tag.asBinaryTag(), () -> null);
    }

    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag the tag to read
     * @throws DataException on a data error
     * @deprecated Use {@link AnvilChunk18#AnvilChunk18(CompoundBinaryTag, Supplier)}
     * @since 2.1.0
     */
    @Deprecated
    public AnvilChunk18(CompoundTag tag, Supplier<CompoundTag> entitiesTag) throws DataException {
        //FAWE start - CBT
        this(tag.asBinaryTag(), () -> {
            CompoundTag compoundTag = entitiesTag.get();
            return compoundTag == null ? null : compoundTag.asBinaryTag();
        });
    }

    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag the tag to read
     * @throws DataException on a data error
     * @since 2.1.0
     */
    public AnvilChunk18(CompoundBinaryTag tag, Supplier<CompoundBinaryTag> entityTag) throws DataException {
        //FAWE end
        rootTag = tag;
        entityTagSupplier = entityTag;

        //FAWE start - CBT
        ListBinaryTag sections = rootTag.getList("sections");
        this.sectionCount = sections.size();

        blocks = new Int2ObjectOpenHashMap<>(sections.size());

        for (BinaryTag rawSectionTag : sections) {
            if (!(rawSectionTag instanceof CompoundBinaryTag sectionTag)) {
                continue;
            }

            int y = NbtUtils.getInt(sectionTag, "Y"); // sometimes a byte, sometimes an int

            BinaryTag rawBlockStatesTag = sectionTag.get("block_states"); // null for sections outside of the world limits
            if (rawBlockStatesTag instanceof CompoundBinaryTag blockStatesTag) {

                // parse palette
                ListBinaryTag paletteEntries = blockStatesTag.getList("palette");
                int paletteSize = paletteEntries.size();
                if (paletteSize == 0) {
                    continue;
                }
                BlockState[] palette = new BlockState[paletteSize];
                for (int paletteEntryId = 0; paletteEntryId < paletteSize; paletteEntryId++) {
                    CompoundBinaryTag paletteEntry = (CompoundBinaryTag) paletteEntries.get(paletteEntryId);
                    BlockType type = BlockTypes.get(paletteEntry.getString("Name"));
                    if (type == null) {
                        throw new InvalidFormatException("Invalid block type: " + paletteEntry.getString("Name"));
                    }
                    BlockState blockState = type.getDefaultState();
                    BinaryTag propertiesTag = paletteEntry.get("Properties");
                    if (propertiesTag instanceof CompoundBinaryTag properties) {
                        for (Property<?> property : blockState.getStates().keySet()) {
                            String value;
                            if (!(value = properties.getString(property.getName())).isEmpty()) {
                                try {
                                    blockState = getBlockStateWith(blockState, property, value);
                                } catch (IllegalArgumentException e) {
                                    throw new InvalidFormatException("Invalid block state for " + blockState
                                            .getBlockType()
                                            .getId() + ", " + property.getName() + ": " + value);
                                }
                            }
                        }
                    }
                    palette[paletteEntryId] = blockState;
                }
                if (paletteSize == 1) {
                    // the same block everywhere
                    blocks.put(y, palette);
                    continue;
                }

                // parse block states
                long[] blockStatesSerialized = blockStatesTag.getLongArray("data");

                BlockState[] chunkSectionBlocks = new BlockState[16 * 16 * 16];
                blocks.put(y, chunkSectionBlocks);

                readBlockStates(palette, blockStatesSerialized, chunkSectionBlocks);
            }
        }
        //FAWE end
    }

    protected void readBlockStates(BlockState[] palette, long[] blockStatesSerialized, BlockState[] chunkSectionBlocks) throws InvalidFormatException {
        PackedIntArrayReader reader = new PackedIntArrayReader(blockStatesSerialized);
        for (int blockPos = 0; blockPos < chunkSectionBlocks.length; blockPos++) {
            int index = reader.get(blockPos);
            if (index >= palette.length) {
                throw new InvalidFormatException("Invalid block state table entry: " + index);
            }
            chunkSectionBlocks[blockPos] = palette[index];
        }
    }

    private <T> BlockState getBlockStateWith(BlockState source, Property<T> property, String value) {
        return source.with(property, property.getValueFor(value));
    }

    /**
     * Used to load the tile entities.
     */
    private void populateTileEntities() throws DataException {
        //FAWE start - CBT
        tileEntities = new HashMap<>();
        if (!(rootTag.get("block_entities") instanceof ListBinaryTag tags)) {
            return;
        }
        for (BinaryTag tag : tags) {
            if (!(tag instanceof CompoundBinaryTag t)) {
                throw new InvalidFormatException("CompoundTag expected in block_entities");
            }

            int x = t.getInt("x");
            int y = t.getInt("y");
            int z = t.getInt("z");

            BlockVector3 vec = BlockVector3.at(x, y, z);
            tileEntities.put(vec, t);
        }
        //FAWE end
    }

    /**
     * Get the map of tags keyed to strings for a block's tile entity data. May
     * return null if there is no tile entity data. Not public yet because
     * what this function returns isn't ideal for usage.
     *
     * @param position the position
     * @return the compound tag for that position, which may be null
     * @throws DataException thrown if there is a data error
     */
    @Nullable
    private CompoundBinaryTag getBlockTileEntity(BlockVector3 position) throws DataException {
        //FAWE start - CBT
        if (tileEntities == null) {
            populateTileEntities();
        }

        return tileEntities.get(position);
        //FAWE end
    }

    @Override
    public BaseBlock getBlock(BlockVector3 position) throws DataException {
        int x = position.x() & 15;
        int y = position.y();
        int z = position.z() & 15;

        int section = y >> 4;
        int yIndex = y & 0x0F;

        BlockState[] sectionBlocks = blocks.get(section);
        if (sectionBlocks == null) {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
        BlockState state = sectionBlocks[sectionBlocks.length == 1 ? 0 : ((yIndex << 8) | (z << 4) | x)];

        CompoundBinaryTag tileEntity = getBlockTileEntity(position);

        if (tileEntity != null) {
            return state.toBaseBlock(tileEntity);
        }

        return state.toBaseBlock();
    }

    @Override
    public BiomeType getBiome(final BlockVector3 position) throws DataException {
        if (biomes == null) {
            populateBiomes();
        }
        int x = (position.x() & 15) >> 2;
        int y = (position.y() & 15) >> 2;
        int z = (position.z() & 15) >> 2;
        int section = position.y() >> 4;
        BiomeType[] sectionBiomes = biomes.get(section);
        if (sectionBiomes.length == 1) {
            return sectionBiomes[0];
        }
        return biomes.get(section)[y << 4 | z << 2 | x];
    }

    private void populateBiomes() throws DataException {
        biomes = new Int2ObjectOpenHashMap<>(sectionCount);
        ListBinaryTag sections = rootTag.getList("sections");
        for (BinaryTag rawSectionTag : sections) {
            if (!(rawSectionTag instanceof CompoundBinaryTag sectionTag)) {
                continue;
            }

            int y = NbtUtils.getInt(sectionTag, "Y"); // sometimes a byte, sometimes an int

            BinaryTag rawBlockStatesTag = sectionTag.get("biomes"); // null for sections outside of the world limits
            if (rawBlockStatesTag instanceof CompoundBinaryTag biomeTypesTag) {

                // parse palette
                ListBinaryTag paletteEntries = biomeTypesTag.getList("palette");
                int paletteSize = paletteEntries.size();
                if (paletteSize == 0) {
                    continue;
                }
                BiomeType[] palette = new BiomeType[paletteSize];
                for (int paletteEntryId = 0; paletteEntryId < paletteSize; paletteEntryId++) {
                    String paletteEntry = paletteEntries.getString(paletteEntryId);
                    BiomeType type = BiomeType.REGISTRY.get(paletteEntry);
                    if (type == null) {
                        throw new InvalidFormatException("Invalid biome type: " + paletteEntry);
                    }
                    palette[paletteEntryId] = type;
                }
                if (paletteSize == 1) {
                    // the same block everywhere
                    biomes.put(y, palette);
                    continue;
                }

                // parse block states
                long[] biomesSerialized = biomeTypesTag.getLongArray("data");
                if (biomesSerialized.length == 0) {
                    throw new InvalidFormatException("Biome data not present.");
                }

                BiomeType[] chunkSectionBiomes = new BiomeType[64];
                biomes.put(y, chunkSectionBiomes);

                readBiomes(palette, biomesSerialized, chunkSectionBiomes);
            }
        }
    }

    protected void readBiomes(BiomeType[] palette, long[] biomesSerialized, BiomeType[] chunkSectionBiomes) throws
            InvalidFormatException {
        PackedIntArrayReader reader = new PackedIntArrayReader(biomesSerialized, 64);
        for (int biomePos = 0; biomePos < chunkSectionBiomes.length; biomePos++) {
            int index = reader.get(biomePos);
            if (index >= palette.length) {
                throw new InvalidFormatException("Invalid biome table entry: " + index);
            }
            chunkSectionBiomes[biomePos] = palette[index];
        }
    }

    @Override
    public List<BaseEntity> getEntities() throws DataException {
        if (entities == null) {
            populateEntities();
        }
        return entities;
    }

    /**
     * Used to load the biomes.
     */
    private void populateEntities() throws DataException {
        entities = new ArrayList<>();
        CompoundBinaryTag entityTag;
        if (entityTagSupplier == null || (entityTag = entityTagSupplier.get()) == null) {
            return;
        }
        ListBinaryTag tags = NbtUtils.getChildTag(entityTag, "Entities", BinaryTagTypes.LIST);

        for (BinaryTag tag : tags) {
            if (!(tag instanceof CompoundBinaryTag t)) {
                throw new InvalidFormatException("CompoundTag expected in Entities");
            }

            entities.add(new BaseEntity(EntityTypes.get(t.getString("id")), LazyReference.computed(t)));
        }

    }


}
