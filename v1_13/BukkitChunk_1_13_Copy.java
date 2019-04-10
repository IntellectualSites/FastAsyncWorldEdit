package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.object.FaweQueue;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.NibbleArray;

public class BukkitChunk_1_13_Copy extends BukkitChunk_1_13 {
    public BukkitChunk_1_13_Copy(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public void set(int i, byte[] ids, byte[] data) {
        this.dataInts[i] = data;
    }

    public boolean storeSection(ChunkSection section, int layer) throws IllegalAccessException {
        if (section == null) {
            return false;
        }
        DataPaletteBlock<IBlockData> blocks = section.getBlocks();
        NibbleArray data = new NibbleArray();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    IBlockData block = blocks.a(x, y, z);
                }
            }
        }
        blocks.set(ids, data);
        set(layer, ids, data.asBytes());
        short solid = (short) getParent().fieldNonEmptyBlockCount.getInt(section);
        count[layer] = solid;
        air[layer] = (short) (4096 - solid);
        return true;
    }

    @Override
    public int[][] getCombinedIdArrays() {
        for (int i = 0; i < ids.length; i++) {
            getIdArray(i);
        }
        return super.getCombinedIdArrays();
    }

    @Override
    public int[] getIdArray(int i) {
        int[] combined = this.ids[i];
        if (combined != null) {
            return combined;
        }
        byte[] idsBytesArray = idsBytes[i];
        if (idsBytesArray == null) {
            return null;
        }
        byte[] datasBytesArray = datasBytes[i];

        idsBytes[i] = null;
        datasBytes[i] = null;

        this.ids[i] = combined = new char[4096];
        for (int j = 0, k = 0; j < 2048; j++, k += 2) {
            combined[k] = (char) (((idsBytesArray[k] & 0xFF) << 4) + (datasBytesArray[j] & 15));
        }
        for (int j = 0, k = 1; j < 2048; j++, k += 2) {
            combined[k] = (char) (((idsBytesArray[k] & 0xFF) << 4) + ((datasBytesArray[j] >> 4) & 15));
        }
        return combined;
    }

    @Override
    public void setBlock(int x, int y, int z, int id) {
        throw new UnsupportedOperationException("This chunk is an immutable copy");
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        throw new UnsupportedOperationException("This chunk is an immutable copy");
    }
}
