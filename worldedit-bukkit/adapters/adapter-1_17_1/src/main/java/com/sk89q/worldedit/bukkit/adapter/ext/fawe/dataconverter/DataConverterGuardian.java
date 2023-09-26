package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterGuardian implements DataConverter {

    public DataConverterGuardian() {
    }

    public int getDataVersion() {
        return 700;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("Guardian".equals(cmp.getString("id"))) {
            if (cmp.getBoolean("Elder")) {
                cmp.putString("id", "ElderGuardian");
            }

            cmp.remove("Elder");
        }

        return cmp;
    }

}
