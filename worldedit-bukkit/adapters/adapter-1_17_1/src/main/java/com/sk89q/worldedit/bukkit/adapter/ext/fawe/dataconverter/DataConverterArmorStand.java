package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterArmorStand implements DataConverter {

    public DataConverterArmorStand() {
    }

    public int getDataVersion() {
        return 147;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("ArmorStand".equals(cmp.getString("id")) && cmp.getBoolean("Silent") && !cmp.getBoolean("Marker")) {
            cmp.remove("Silent");
        }

        return cmp;
    }

}
