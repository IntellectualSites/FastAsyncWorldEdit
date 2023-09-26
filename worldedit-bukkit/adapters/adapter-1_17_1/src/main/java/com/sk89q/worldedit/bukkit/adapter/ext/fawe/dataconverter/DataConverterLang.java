package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import java.util.Locale;

public class DataConverterLang implements DataConverter {

    public DataConverterLang() {
    }

    public int getDataVersion() {
        return 816;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if (cmp.contains("lang", 8)) {
            cmp.putString("lang", cmp.getString("lang").toLowerCase(Locale.ROOT));
        }

        return cmp;
    }

}
