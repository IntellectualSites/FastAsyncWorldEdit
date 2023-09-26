package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterShulker implements DataConverter {

    public DataConverterShulker() {
    }

    public int getDataVersion() {
        return 808;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:shulker".equals(cmp.getString("id")) && !cmp.contains("Color", 99)) {
            cmp.putByte("Color", (byte) 10);
        }

        return cmp;
    }

}
