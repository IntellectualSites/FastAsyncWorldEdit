package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterTotem implements DataConverter {

    public DataConverterTotem() {
    }

    public int getDataVersion() {
        return 820;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:totem".equals(cmp.getString("id"))) {
            cmp.putString("id", "minecraft:totem_of_undying");
        }

        return cmp;
    }

}
