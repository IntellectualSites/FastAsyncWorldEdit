package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import java.util.UUID;

public class DataConverterUUID implements DataConverter {

    public DataConverterUUID() {
    }

    public int getDataVersion() {
        return 108;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if (cmp.contains("UUID", 8)) {
            cmp.putUUID("UUID", UUID.fromString(cmp.getString("UUID")));
        }

        return cmp;
    }

}
