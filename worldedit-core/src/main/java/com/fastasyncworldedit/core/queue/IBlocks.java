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

    char[] load(int layer);

    BlockState getBlock(int x, int y, int z);

    Map<BlockVector3, CompoundTag> getTiles();

    CompoundTag getTile(int x, int y, int z);

    Set<CompoundTag> getEntities();

    BiomeType getBiomeType(int x, int y, int z);

    default int getBitMask() {
        return IntStream.range(getMinSectionIndex(), getMaxSectionIndex() + 1).filter(this::hasSection)
                .map(layer -> (1 << layer)).sum();
    }

    void removeSectionLighting(int layer, boolean sky);

    boolean trim(boolean aggressive, int layer);

    IBlocks reset();

    /**
     * Get the number of stores sections
     */
    int getSectionCount();

    /**
     * Max ChunkSection array index
     */
    int getMaxSectionIndex();

    /**
     * Min ChunkSection array index
     */
    int getMinSectionIndex();

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
