package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterShulkerBoxBlock implements DataConverter {

    public DataConverterShulkerBoxBlock() {
    }

    public int getDataVersion() {
        return 813;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:shulker".equals(cmp.getString("id"))) {
            cmp.remove("Color");
        }

        return cmp;
    }

}
