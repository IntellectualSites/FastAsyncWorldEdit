package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import com.google.common.collect.Lists;

import java.util.List;

public class DataConverterMinecart implements DataConverter {

    private static final List<String> CART_TYPES = Lists.newArrayList(
            "MinecartRideable",
            "MinecartChest",
            "MinecartFurnace",
            "MinecartTNT",
            "MinecartSpawner",
            "MinecartHopper",
            "MinecartCommandBlock"
    );

    public DataConverterMinecart() {
    }

    public int getDataVersion() {
        return 106;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("Minecart".equals(cmp.getString("id"))) {
            String s = "MinecartRideable";
            int i = cmp.getInt("Type");

            if (i > 0 && i < DataConverterMinecart.CART_TYPES.size()) {
                s = DataConverterMinecart.CART_TYPES.get(i);
            }

            cmp.putString("id", s);
            cmp.remove("Type");
        }

        return cmp;
    }

}
