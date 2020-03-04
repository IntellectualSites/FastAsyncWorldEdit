package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.jetbrains.annotations.Range;

/**
 * Shared interface for IGetBlocks and ISetBlocks
 */
public interface IBlocks extends Trimable {

    boolean hasSection(@Range(from = 0, to = 15) int layer);

    char[] load(int layer);

    BlockState getBlock(int x, int y, int z);

    Map<BlockVector3, CompoundTag> getTiles();

    CompoundTag getTile(int x, int y, int z);

    Set<CompoundTag> getEntities();

    BiomeType getBiomeType(int x, int y, int z);

    default int getBitMask() {
        return IntStream.range(0, FaweCache.IMP.CHUNK_LAYERS).filter(this::hasSection)
            .map(layer -> (1 << layer)).sum();
    }

    IBlocks reset();

    default byte[] toByteArray(boolean full) {
        return toByteArray(null, getBitMask(), full);
    }

    default byte[] toByteArray(byte[] buffer, int bitMask, boolean full) {
        if (buffer == null) {
            buffer = new byte[1024];
        }

        BlockRegistry registry = WorldEdit.getInstance().getPlatformManager()
            .queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry();
        FastByteArrayOutputStream sectionByteArray = new FastByteArrayOutputStream(buffer);
        try (FaweOutputStream sectionWriter = new FaweOutputStream(sectionByteArray)) {
            for (int layer = 0; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
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
                FaweCache.Palette palette = FaweCache.IMP.toPalette(0, ids);

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
