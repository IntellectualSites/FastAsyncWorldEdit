package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.bukkit.adapter.v1_13_1.Spigot_v1_13_R2;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataBits;
import net.minecraft.server.v1_13_R2.DataPalette;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.IBlockData;

import static com.boydti.fawe.bukkit.v0.BukkitQueue_0.getAdapter;

public class BukkitChunk_1_13_Copy extends BukkitChunk_1_13 {
    public BukkitChunk_1_13_Copy(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public int[][] getCombinedIdArrays() {
        if (this.sectionPalettes == null) {
            return this.ids;
        }
        for (int i = 0; i < ids.length; i++) {
            getIdArray(i);
        }
        return super.getCombinedIdArrays();
    }

    @Override
    public int[] getIdArray(int layer) {
        if (this.sectionPalettes != null) {
            ChunkSection section = this.sectionPalettes[layer];
            int[] idsArray = this.ids[layer];
            if (section != null && idsArray == null) {
                idsArray = new int[4096];
                if (!section.a()) {
                    try {
                        DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                        DataBits bits = (DataBits) BukkitQueue_1_13.fieldBits.get(blocks);
                        DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitQueue_1_13.fieldPalette.get(blocks);

                        long[] raw = bits.a();
                        int bitsPerEntry = bits.c();

                        new BitArray4096(raw, bitsPerEntry).toRaw(idsArray);
                        IBlockData defaultBlock = (IBlockData) BukkitQueue_1_13.fieldDefaultBlock.get(blocks);
                        // TODO optimize away palette.a
                        for (int i = 0; i < 4096; i++) {
                            IBlockData ibd = palette.a(idsArray[i]);
                            if (ibd == null) {
                                ibd = defaultBlock;
                            }
                            int ordinal = ((Spigot_v1_13_R2) getAdapter()).adaptToInt(ibd);
                            idsArray[i] = BlockTypes.states[ordinal].getInternalId();
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            return idsArray;
        }
        return null;
    }

    @Override
    public <B extends BlockStateHolder<B>> void setBlock(int x, int y, int z, B block) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public void setBiome(BiomeType biome) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {
        throw new UnsupportedOperationException("Read only");
    }
}
