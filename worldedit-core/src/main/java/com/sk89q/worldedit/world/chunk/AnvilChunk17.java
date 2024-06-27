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
import com.google.common.collect.ImmutableMap;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.storage.InvalidFormatException;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The chunk format for Minecraft 1.17
 */
public class AnvilChunk17 implements Chunk {

    private final LinCompoundTag rootTag;
    private final Supplier<LinCompoundTag> entityTagSupplier;
    private BiomeType[] biomes;
    private BlockState[][] blocks;
    private Map<BlockVector3, LinCompoundTag> tileEntities;
    private List<BaseEntity> entities;
    // initialise with default values
    private int minSectionPosition = 0;
    private int maxSectionPosition = 15;
    private int sectionCount = 16;

    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag the tag to read
     * @throws DataException on a data error
     * @deprecated Use {@link #AnvilChunk17(LinCompoundTag, Supplier)}
     */
    @Deprecated
    public AnvilChunk17(CompoundTag tag, Supplier<CompoundTag> entitiesTag) throws DataException {
        this(tag.toLinTag(), () -> {
            CompoundTag compoundTag = entitiesTag.get();
            if (compoundTag == null) {
                return null;
            }
            return compoundTag.toLinTag();
        });
    }

    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag       the tag to read
     * @param entityTag supplier for the entity compound tag found in the entities folder mca files. Not accessed unless
     *                  {@link #getEntities()} is called
     * @throws DataException on a data error
     */
    public AnvilChunk17(LinCompoundTag tag, Supplier<LinCompoundTag> entityTag) throws DataException {
        rootTag = tag;
        entityTagSupplier = entityTag;

        blocks = new BlockState[16][]; // initialise with default length

        LinListTag<LinTag<?>> sections = rootTag.getTag("Sections", LinTagType.listTag());

        for (LinTag<?> rawSectionTag : sections.value()) {
            if (!(rawSectionTag instanceof LinCompoundTag sectionTag)) {
                continue;
            }

            var sectionYTag = sectionTag.findTag("Y", LinTagType.byteTag());
            if (sectionYTag == null) {
                continue; // Empty section.
            }

            int y = NbtUtils.getInt(sectionTag, "Y");
            updateSectionIndexRange(y);

            // parse palette
            LinListTag paletteEntries = sectionTag.getListTag("Palette", LinTagType.compoundTag());
            int paletteSize = paletteEntries.value().size();
            if (paletteSize == 0) {
                continue;
            }
            BlockState[] palette = new BlockState[paletteSize];
            for (int paletteEntryId = 0; paletteEntryId < paletteSize; paletteEntryId++) {
                LinCompoundTag paletteEntry = (LinCompoundTag) paletteEntries.get(paletteEntryId);
                BlockType type = BlockTypes.get(paletteEntry.getTag("Name", LinTagType.stringTag()).value());
                if (type == null) {
                    throw new InvalidFormatException("Invalid block type: " + paletteEntry
                            .getTag("Name", LinTagType.stringTag())
                            .value());
                }
                BlockState blockState = type.getDefaultState();
                LinCompoundTag properties = paletteEntry.findTag("Properties", LinTagType.compoundTag());
                if (properties != null) {
                    for (Property<?> property : blockState.getStates().keySet()) {
                        LinStringTag stringTag = properties.findTag(property.getName(), LinTagType.stringTag());
                        if (stringTag != null) {
                            try {
                                blockState = getBlockStateWith(blockState, property, stringTag.value());
                            } catch (IllegalArgumentException e) {
                                throw new InvalidFormatException("Invalid block state for " + blockState
                                        .getBlockType()
                                        .id() + ", " + property.getName() + ": " + stringTag.value());
                            }
                        }
                    }
                }
                palette[paletteEntryId] = blockState;
            }

            // parse block states
            long[] blockStatesSerialized = sectionTag.getTag("BlockStates", LinTagType.longArrayTag()).value();

            BlockState[] chunkSectionBlocks = new BlockState[4096];
            blocks[y - minSectionPosition] = chunkSectionBlocks;

            readBlockStates(palette, blockStatesSerialized, chunkSectionBlocks);
        }
    }

    private void updateSectionIndexRange(int layer) {
        if (layer >= minSectionPosition && layer <= maxSectionPosition) {
            return;
        }
        if (layer < minSectionPosition) {
            int diff = minSectionPosition - layer;
            sectionCount += diff;
            BlockState[][] tmpBlocks = new BlockState[sectionCount][];
            System.arraycopy(blocks, 0, tmpBlocks, diff, blocks.length);
            blocks = tmpBlocks;
            minSectionPosition = layer;
        } else {
            int diff = layer - maxSectionPosition;
            sectionCount += diff;
            BlockState[][] tmpBlocks = new BlockState[sectionCount][];
            System.arraycopy(blocks, 0, tmpBlocks, 0, blocks.length);
            blocks = tmpBlocks;
            maxSectionPosition = layer;
        }
    }

    protected void readBlockStates(BlockState[] palette, long[] blockStatesSerialized, BlockState[] chunkSectionBlocks) throws
            InvalidFormatException {
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
    private void populateTileEntities() {
        LinListTag<LinCompoundTag> tags = rootTag.findListTag(
                "TileEntities", LinTagType.compoundTag()
        );
        if (tags == null) {
            tileEntities = ImmutableMap.of();
            return;
        }
        var tileEntitiesBuilder = ImmutableMap.<BlockVector3, LinCompoundTag>builderWithExpectedSize(tags.value().size());
        for (LinCompoundTag tag : tags.value()) {
            int x = tag.getTag("x", LinTagType.intTag()).valueAsInt();
            int y = tag.getTag("y", LinTagType.intTag()).valueAsInt();
            int z = tag.getTag("z", LinTagType.intTag()).valueAsInt();

            BlockVector3 vec = BlockVector3.at(x, y, z);
            tileEntities.put(vec, tag);
        }
        tileEntities = tileEntitiesBuilder.build();
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
    private LinCompoundTag getBlockTileEntity(BlockVector3 position) throws DataException {
        if (tileEntities == null) {
            populateTileEntities();
        }

        return tileEntities.get(position);
    }

    @Override
    public BaseBlock getBlock(BlockVector3 position) throws DataException {
        int x = position.x() & 15;
        int y = position.y();
        int z = position.z() & 15;

        int section = y >> 4;
        int yIndex = y & 0x0F;

        if (section < minSectionPosition || section > maxSectionPosition) {
            throw new DataException("Chunk does not contain position " + position);
        }

        BlockState[] sectionBlocks = blocks[section - minSectionPosition];
        BlockState state = sectionBlocks != null ? sectionBlocks[(yIndex << 8) | (z << 4) | x] : BlockTypes.AIR.getDefaultState();

        LinCompoundTag tileEntity = getBlockTileEntity(position);

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
        int y = (position.y() - (minSectionPosition << 4)) >> 2; // normalize
        int z = (position.z() & 15) >> 2;
        return biomes[y << 4 | z << 2 | x];
    }

    private void populateBiomes() throws DataException {
        biomes = new BiomeType[64 * blocks.length];
        LinIntArrayTag biomeTag = rootTag.findTag("Biomes", LinTagType.intArrayTag());
        if (biomeTag == null) {
            return;
        }
        int[] stored = biomeTag.value();
        for (int i = 0; i < 1024; i++) {
            biomes[i] = BiomeTypes.getLegacy(stored[i]);
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
    private void populateEntities() {
        entities = new ArrayList<>();
        LinListTag<LinCompoundTag> tags = rootTag.findListTag(
                "Entities", LinTagType.compoundTag()
        );
        if (tags == null) {
            return;
        }

        for (LinCompoundTag tag : tags.value()) {

            entities.add(new BaseEntity(
                    EntityTypes.get(tag.getTag("id", LinTagType.stringTag()).value()),
                    LazyReference.computed(tag)
            ));
        }

    }

}
