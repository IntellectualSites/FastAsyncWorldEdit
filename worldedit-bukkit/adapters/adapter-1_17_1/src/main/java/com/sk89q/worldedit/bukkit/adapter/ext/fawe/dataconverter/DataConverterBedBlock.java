package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataConverterBedBlock implements DataConverter {

    private static final Logger LOGGER = LogManager.getLogger(DataConverterBedBlock.class);

    public DataConverterBedBlock() {
    }

    public int getDataVersion() {
        return 1125;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        try {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("Level");
            int i = nbttagcompound1.getInt("xPos");
            int j = nbttagcompound1.getInt("zPos");
            net.minecraft.nbt.ListTag nbttaglist = nbttagcompound1.getList("TileEntities", 10);
            net.minecraft.nbt.ListTag nbttaglist1 = nbttagcompound1.getList("Sections", 10);

            for (int k = 0; k < nbttaglist1.size(); ++k) {
                net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttaglist1.getCompound(k);
                byte b0 = nbttagcompound2.getByte("Y");
                byte[] abyte = nbttagcompound2.getByteArray("Blocks");

                for (int l = 0; l < abyte.length; ++l) {
                    if (416 == (abyte[l] & 255) << 4) {
                        int i1 = l & 15;
                        int j1 = l >> 8 & 15;
                        int k1 = l >> 4 & 15;
                        net.minecraft.nbt.CompoundTag nbttagcompound3 = new net.minecraft.nbt.CompoundTag();

                        nbttagcompound3.putString("id", "bed");
                        nbttagcompound3.putInt("x", i1 + (i << 4));
                        nbttagcompound3.putInt("y", j1 + (b0 << 4));
                        nbttagcompound3.putInt("z", k1 + (j << 4));
                        nbttaglist.add(nbttagcompound3);
                    }
                }
            }
        } catch (Exception exception) {
            DataConverterBedBlock.LOGGER.warn("Unable to datafix Bed blocks, level format may be missing tags.");
        }

        return cmp;
    }

}
