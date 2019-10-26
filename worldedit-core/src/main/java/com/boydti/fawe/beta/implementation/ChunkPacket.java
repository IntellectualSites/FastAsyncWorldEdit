package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockRegistry;

import java.util.function.Function;
import java.util.function.Supplier;

public class ChunkPacket implements Function<byte[], byte[]>, Supplier<byte[]> {

    private final boolean full;
    private final boolean biomes;
    private final IBlocks chunk;
    private final int chunkX;
    private final int chunkZ;

    public ChunkPacket(int chunkX, int chunkZ, IBlocks chunk, boolean replaceAllSections, boolean sendBiomeData) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunk = chunk;
        this.full = replaceAllSections;
        this.biomes = sendBiomeData;
    }

    @Override
    @Deprecated
    public byte[] get() {
        System.out.println("TODO deprecated, use buffer");
        return apply(new byte[8192]);
    }

    @Override
    public byte[] apply(byte[] buffer) {
        try {
            FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
            FaweOutputStream fos = new FaweOutputStream(baos);

            fos.writeInt(this.chunkX);
            fos.writeInt(this.chunkZ);

            fos.writeBoolean(this.full);

            fos.writeVarInt(this.chunk.getBitMask()); // writeVarInt

            // TODO write NBTTagCompound of HeightMaps
            fos.writeVarInt(0); // (Entities / NBT)

            // TODO write chunk data to byte[]
            {
                BlockRegistry registry = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry();
                try (FastByteArrayOutputStream sectionByteArray = new FastByteArrayOutputStream(buffer); FaweOutputStream sectionWriter = new FaweOutputStream(sectionByteArray)) {
                    for (int layer = 0; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
                        if (!this.chunk.hasSection(layer)) continue;
                        char[] ids = this.chunk.getArray(layer);
                        FaweCache.Palette palette = FaweCache.IMP.toPalette(0, ids);

                        int nonEmpty = 0; // TODO optimize into same loop as toPalette
                        for (char id :ids) {
                            if (id != 0) nonEmpty++;
                        }
                        sectionWriter.writeShort(nonEmpty); // non empty
                        sectionWriter.writeByte(palette.bitsPerEntry); // bits per block
                        sectionWriter.writeVarInt(palette.paletteToBlockLength);
                        for (int i = 0; i < palette.paletteToBlockLength; i++) {
                            int ordinal = palette.paletteToBlock[i];
                            switch (ordinal) {
                                case BlockID.CAVE_AIR:
                                case BlockID.VOID_AIR:
                                case BlockID.AIR:
                                case BlockID.__RESERVED__:
                                    sectionWriter.writeByte(0);
                                    break;
                                default:
                                    BlockState state = BlockState.getFromOrdinal(ordinal);
                                    int mcId = registry.getInternalBlockStateId(state).getAsInt();
                                    sectionWriter.writeVarInt(mcId);
                            }
                        }
                        sectionWriter.writeVarInt(palette.blockStatesLength);
                        for (int i = 0; i < palette.blockStatesLength; i++) {
                            sectionWriter.writeLong(palette.blockStates[i]);
                        }
                    }

                    // TODO write biomes
//                    boolean writeBiomes = true;
//                    for (int x = 0; x < 16; x++) {
//                        for (int z = 0; z < 16; z++) {
//                            BiomeType biome = this.chunk.getBiomeType(x, z);
//                            if (biome == null) {
//                                if (writeBiomes) {
//                                    break;
//                                } else {
//                                    biome = BiomeTypes.FOREST;
//                                }
//                            }
//                        }
//                    }
                    if (this.full) {
                        for (int i = 0; i < 256; i++) {
                            sectionWriter.writeInt(0);
                        }
                    }

                    fos.writeVarInt(sectionByteArray.getSize());
                    for (byte[] arr : sectionByteArray.toByteArrays()) {
                        fos.write(arr);
                    }
                }
            }
            // TODO entities / NBT
            fos.writeVarInt(0); // (Entities / NBT)
            return baos.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
