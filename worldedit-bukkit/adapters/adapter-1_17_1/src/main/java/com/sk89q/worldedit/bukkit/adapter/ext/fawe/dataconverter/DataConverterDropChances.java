package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterDropChances implements DataConverter {
    public DataConverterDropChances() {
    }

    public int getDataVersion() {
        return 113;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        net.minecraft.nbt.ListTag nbttaglist;

        if (cmp.contains("HandDropChances", 9)) {
            nbttaglist = cmp.getList("HandDropChances", 5);
            if (nbttaglist.size() == 2 && nbttaglist.getFloat(0) == 0.0F && nbttaglist.getFloat(1) == 0.0F) {
                cmp.remove("HandDropChances");
            }
        }

        if (cmp.contains("ArmorDropChances", 9)) {
            nbttaglist = cmp.getList("ArmorDropChances", 5);
            if (nbttaglist.size() == 4 && nbttaglist.getFloat(0) == 0.0F && nbttaglist.getFloat(1) == 0.0F && nbttaglist.getFloat(
                    2) == 0.0F && nbttaglist.getFloat(3) == 0.0F) {
                cmp.remove("ArmorDropChances");
            }
        }

        return cmp;
    }
}
