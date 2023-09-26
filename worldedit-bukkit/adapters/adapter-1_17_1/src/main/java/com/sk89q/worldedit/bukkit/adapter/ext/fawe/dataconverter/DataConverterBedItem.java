package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import net.minecraft.world.item.DyeColor;

public class DataConverterBedItem implements DataConverter {

    public DataConverterBedItem() {
    }

    public int getDataVersion() {
        return 1125;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:bed".equals(cmp.getString("id")) && cmp.getShort("Damage") == 0) {
            cmp.putShort("Damage", (short) DyeColor.RED.getId());
        }

        return cmp;
    }

}
