package com.boydti.fawe.beta;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.collection.BitArray4096;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Shared interface for IGetBlocks and ISetBlocks
 */
public interface IBlocks extends Trimable {

    boolean hasSection(int layer);

    char[] getArray(int layer);

    BlockState getBlock(int x, int y, int z);

    Map<BlockVector3, CompoundTag> getTiles();

    Set<CompoundTag> getEntities();

    BiomeType getBiomeType(int x, int z);

    default int getBitMask() {
        int mask = 0;
        for (int layer = 0; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
            if (hasSection(layer)) {
                mask += (1 << layer);
            }
        }
        return mask;
    }

    IBlocks reset();

    default byte[] toByteArray(boolean writeBiomes) {
        return toByteArray(null, writeBiomes);
    }

    default byte[] toByteArray(byte[] buffer, boolean writeBiomes) {
        if (buffer == null) {
            buffer = new byte[1024];
        }

        BlockRegistry registry = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry();
        FastByteArrayOutputStream sectionByteArray = new FastByteArrayOutputStream(buffer);
        FaweOutputStream sectionWriter = new FaweOutputStream(sectionByteArray);
        try {
            for (int layer = 0; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
                if (!this.hasSection(layer)) continue;

                char[] ids = this.getArray(layer);

                int nonEmpty = 0; // TODO optimize into same loop as toPalette
                for (char id : ids) {
                    if (id != 0) nonEmpty++;
                }

                sectionWriter.writeShort(nonEmpty); // non empty

                sectionWriter.writeByte(14); // globalPaletteBitsPerBlock

                if (true) {
                    BitArray4096 bits = new BitArray4096(14); // globalPaletteBitsPerBlock
                    bits.setAt(0, 0);
                    for (int i = 0; i < 4096; i++) {
                        int ordinal = ids[i];
                        BlockState state = BlockState.getFromOrdinal(ordinal);
                        if (!state.getMaterial().isAir()) {
                            int mcId = registry.getInternalBlockStateId(state).getAsInt();
                            bits.setAt(i, mcId);
                        }
                    }
                    sectionWriter.write(bits.getData());
                } else {

                    FaweCache.Palette palette = FaweCache.IMP.toPalette(0, ids);

                    sectionWriter.writeByte(palette.bitsPerEntry); // bits per block
                    sectionWriter.writeVarInt(palette.paletteToBlockLength);
                    for (int i = 0; i < palette.paletteToBlockLength; i++) {
                        int ordinal = palette.paletteToBlock[i];
                        switch (ordinal) {
                            case BlockID.CAVE_AIR:
                            case BlockID.VOID_AIR:
                            case BlockID.AIR:
                            case BlockID.__RESERVED__:
                                sectionWriter.writeVarInt(0);
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
            }

//        if (writeBiomes) {
//            for (int x = 0; x < 16; x++) {
//                for (int z = 0; z < 16; z++) {
//                    BiomeType biome = this.getBiomeType(x, z);
//                    if (biome == null) {
//                        if (writeBiomes) {
//                            break;
//                        } else {
//                            biome = BiomeTypes.FOREST;
//                        }
//                    }
//                }
//            }
//        }
            if (writeBiomes) {
                for (int i = 0; i < 256; i++) {
                    // TODO biomes
                    sectionWriter.writeInt(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sectionByteArray.toByteArray();
    }
}
