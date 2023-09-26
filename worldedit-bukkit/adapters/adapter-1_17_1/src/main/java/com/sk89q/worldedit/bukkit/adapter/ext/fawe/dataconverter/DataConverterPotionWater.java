package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterPotionWater implements DataConverter {

    public DataConverterPotionWater() {
    }

    public int getDataVersion() {
        return 806;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        String s = cmp.getString("id");

        if ("minecraft:potion".equals(s) || "minecraft:splash_potion".equals(s) || "minecraft:lingering_potion".equals(s) || "minecraft:tipped_arrow".equals(
                s)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");

            if (!nbttagcompound1.contains("Potion", 8)) {
                nbttagcompound1.putString("Potion", "minecraft:water");
            }

            if (!cmp.contains("tag", 10)) {
                cmp.put("tag", nbttagcompound1);
            }
        }

        return cmp;
    }

}
