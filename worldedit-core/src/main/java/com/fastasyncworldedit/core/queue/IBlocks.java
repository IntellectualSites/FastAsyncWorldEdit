package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.internal.io.FastByteArrayOutputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.world.block.BlockID;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockRegistry;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * A shared interface for IGetBlocks and ISetBlocks.
 */
public interface IBlocks extends Trimable {

    /**
     * Returns if the chunk has a BLOCKS section at the given layer. May not be indicative of presence
     * of entities, tile entites, biomes, etc.
     *
     * @param layer chunk section layer
     * @return if blocks/a block section is present
     */
    boolean hasSection(int layer);

    /**
     * Obtain the specified chunk section stored as an array of ordinals. Uses normal minecraft chunk-section position indices
     * (length 4096). Operations synchronises on the section and will load the section into memory if not present. For chunk
     * GET operations, this will load the data from the world. For chunk SET, this will create a new empty array.
     *
     * @param layer chunk section layer (may be negative)
     * @return char array of ordinals of the chunk section
     */
    char[] load(int layer);

    /**
     * Obtain the specified chunk section stored as an array of ordinals if present or null. Uses normal minecraft chunk-section
     * position indices (length 4096). Does not synchronise to the section layer as it will not attempt to load into memory.
     *
     * @param layer chunk section layer (may be negative)
     * @return char array of ordinals of the chunk section if present
     */
    @Nullable
    char[] loadIfPresent(int layer);

    BlockState getBlock(int x, int y, int z);

    Map<BlockVector3, CompoundTag> getTiles();

    CompoundTag getTile(int x, int y, int z);

    Set<CompoundTag> getEntities();

    BiomeType getBiomeType(int x, int y, int z);

    default int getBitMask() {
        return IntStream.range(getMinSectionPosition(), getMaxSectionPosition() + 1).filter(this::hasSection)
                .map(layer -> (1 << layer)).sum();
    }

    void removeSectionLighting(int layer, boolean sky);

    boolean trim(boolean aggressive, int layer);

    IBlocks reset();

    /**
     * Get the number of stored sections
     */
    int getSectionCount();

    /**
     * Get the highest layer position stored in the internal chunk. For 1.16 and below, always returns 15. For 1.17 and above, may
     * not return a value correct to the world if this is a {@link IChunkSet} instance, which defaults to 15. For extended
     * height worlds, this will only return over 15 if blocks are stored outside the default range.
     */
    int getMaxSectionPosition();

    /**
     * Get the lowest layer position stored in the internal chunk. For 1.16 and below, always returns 0. For 1.17 and above, may
     * not return a value correct to the world if this is a {@link IChunkSet} instance, which defaults to 0. For extended
     * height worlds, this will only return under 0 if blocks are stored outside the default range.
     */
    int getMinSectionPosition();

    default byte[] toByteArray(boolean full, boolean stretched) {
        return toByteArray(null, getBitMask(), full, stretched);
    }

    default byte[] toByteArray(byte[] buffer, int bitMask, boolean full, boolean stretched) {
        if (buffer == null) {
            buffer = new byte[1024];
        }

        BlockRegistry registry = WorldEdit.getInstance().getPlatformManager()
                .queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry();
        FastByteArrayOutputStream sectionByteArray = new FastByteArrayOutputStream(buffer);
        try (FaweOutputStream sectionWriter = new FaweOutputStream(sectionByteArray)) {
            for (int layer = 0; layer < this.getSectionCount(); layer++) {
                if (!this.hasSection(layer) || (bitMask & (1 << layer)) == 0) {
                    continue;
                }

                char[] ids = this.load(layer);

                int nonEmpty = 0; // TODO optimize into same loop as toPalette
                for (int i = 0; i < ids.length; i++) {
                    char ordinal = ids[i];
                    switch (ordinal) {
                        case BlockID.__RESERVED__:
                        case BlockID.CAVE_AIR:
                        case BlockID.VOID_AIR:
                            ids[i] = BlockID.AIR;
                        case BlockID.AIR:
                            continue;
                        default:
                            nonEmpty++;
                    }
                }

                sectionWriter.writeShort(nonEmpty); // non empty
                FaweCache.Palette palette;
                if (stretched) {
                    palette = FaweCache.IMP.toPalette(0, ids);
                } else {
                    palette = FaweCache.IMP.toPaletteUnstretched(0, ids);
                }

                sectionWriter.writeByte(palette.bitsPerEntry); // bits per block
                sectionWriter.writeVarInt(palette.paletteToBlockLength);
                for (int i = 0; i < palette.paletteToBlockLength; i++) {
                    int ordinal = palette.paletteToBlock[i];
                    switch (ordinal) {
                        case BlockID.__RESERVED__:
                        case BlockID.CAVE_AIR:
                        case BlockID.VOID_AIR:
                        case BlockID.AIR:
                            sectionWriter.write(0);
                            break;
                        default:
                            BlockState state = BlockState.getFromOrdinal(ordinal);
                            int mcId = registry.getInternalBlockStateId(state).getAsInt();
                            sectionWriter.writeVarInt(mcId);
                            break;
                    }
                }
                sectionWriter.writeVarInt(palette.blockStatesLength);
                for (int i = 0; i < palette.blockStatesLength; i++) {
                    sectionWriter.writeLong(palette.blockStates[i]);
                }
            }
            if (full) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BiomeType biome = getBiomeType(x, 0, z);
                        if (biome != null) {
                            sectionWriter.writeInt(biome.getLegacyId());
                        } else {
                            sectionWriter.writeInt(0);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sectionByteArray.toByteArray();
    }

}
