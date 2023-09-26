package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterVBO implements DataConverter {

    public DataConverterVBO() {
    }

    public int getDataVersion() {
        return 505;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        cmp.putString("useVbo", "true");
        return cmp;
    }

}
