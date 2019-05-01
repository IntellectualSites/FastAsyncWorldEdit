package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.boydti.fawe.bukkit.adapter.v1_13_1.Spigot_v1_13_R2;
import com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataBits;
import net.minecraft.server.v1_13_R2.DataPalette;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.DataPaletteHash;
import net.minecraft.server.v1_13_R2.DataPaletteLinear;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.World;

import static com.boydti.fawe.bukkit.v0.BukkitQueue_0.getAdapter;

public class BukkitGetBlocks extends CharGetBlocks {
    public ChunkSection[] sections;
    public Chunk nmsChunk;
    public World nmsWorld;
    public int X, Z;

    public BukkitGetBlocks(World nmsWorld, int X, int Z) {/*d*/
        this.nmsWorld = nmsWorld;
        this.X = X;
        this.Z = Z;
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        // TODO
        return null;
    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        // TODO
        return null;
    }

    @Override
    public char[] load(int layer) {
        return load(layer, null);
    }

    @Override
    public char[] load(int layer, char[] data) {
        ChunkSection section = getSections()[layer];
        // Section is null, return empty array
        if (section == null) {
            return FaweCache.EMPTY_CHAR_4096;
        }
        if (data == null || data == FaweCache.EMPTY_CHAR_4096) {
            data = new char[4096];
        }
        DelegateLock lock = BukkitQueue.applyLock(section);
        synchronized (lock) {
            if (lock.isLocked()) {
                lock.lock();
                lock.unlock();
            }
            lock.setModified(false);
            // Efficiently convert ChunkSection to raw data
            try {
                final DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                final DataBits bits = (DataBits) BukkitQueue_1_13.fieldBits.get(blocks);
                final DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitQueue_1_13.fieldPalette.get(blocks);
                final int bitsPerEntry = bits.c();

                final long[] blockStates = bits.a();
                new BitArray4096(blockStates, bitsPerEntry).toRaw(data);

                int num_palette;
                if (palette instanceof DataPaletteLinear) {
                    num_palette = ((DataPaletteLinear<IBlockData>) palette).b();
                } else if (palette instanceof DataPaletteHash) {
                    num_palette = ((DataPaletteHash<IBlockData>) palette).b();
                } else {
                    num_palette = 0;
                    int[] paletteToBlockInts = FaweCache.PALETTE_TO_BLOCK.get();
                    char[] paletteToBlockChars = FaweCache.PALETTE_TO_BLOCK_CHAR.get();
                    try {
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char ordinal = paletteToBlockChars[paletteVal];
                            if (ordinal == Character.MAX_VALUE) {
                                paletteToBlockInts[num_palette++] = paletteVal;
                                IBlockData ibd = palette.a(data[i]);
                                if (ibd == null) {
                                    ordinal = BlockTypes.AIR.getDefaultState().getOrdinalChar();
                                } else {
                                    ordinal = ((Spigot_v1_13_R2) getAdapter()).adaptToChar(ibd);
                                }
                                paletteToBlockChars[paletteVal] = ordinal;
                            }
                            data[i] = ordinal;
                        }
                    } finally {
                        for (int i = 0; i < num_palette; i++) {
                            int paletteVal = paletteToBlockInts[i];
                            paletteToBlockChars[paletteVal] = Character.MAX_VALUE;
                        }
                    }
                    return data;
                }

                char[] paletteToBlockChars = FaweCache.PALETTE_TO_BLOCK_CHAR.get();
                try {
                    for (int i = 0; i < num_palette; i++) {
                        IBlockData ibd = palette.a(i);
                        char ordinal;
                        if (ibd == null) {
                            ordinal = BlockTypes.AIR.getDefaultState().getOrdinalChar();
                            System.out.println("Invalid palette");
                        } else {
                            ordinal = ((Spigot_v1_13_R2) getAdapter()).adaptToChar(ibd);
                        }
                        paletteToBlockChars[i] = ordinal;
                    }
                    for (int i = 0; i < 4096; i++) {
                        char paletteVal = data[i];
                        data[i] = paletteToBlockChars[paletteVal];
                    }
                } finally {
                    for (int i = 0; i < num_palette; i++) {
                        paletteToBlockChars[i] = Character.MAX_VALUE;
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return data;
        }
    }

    public ChunkSection[] getSections() {
        ChunkSection[] tmp = sections;
        if (tmp == null) {
            synchronized (this) {
                tmp = sections;
                if (tmp == null) {
                    Chunk chunk = getChunk();
                    sections = tmp = chunk.getSections().clone();
                }
            }
        }
        return tmp;
    }

    public Chunk getChunk() {
        Chunk tmp = nmsChunk;
        if (tmp == null) {
            synchronized (this) {
                tmp = nmsChunk;
                if (tmp == null) {
                    nmsChunk = tmp = BukkitQueue.ensureLoaded(nmsWorld, X, Z);
                }
            }
        }
        return tmp;
    }

    @Override
    public boolean hasSection(int layer) {
        return getSections()[layer] != null;
    }

    @Override
    public boolean trim(boolean aggressive) {
        if (aggressive) {
            sections = null;
            nmsChunk = null;
        }
        return super.trim(aggressive);
    }
}
