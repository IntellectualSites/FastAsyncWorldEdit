package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterShulkerBoxItem implements DataConverter {

    public static final String[] BOX_TYPES = new String[]{"minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box", "minecraft:black_shulker_box"};

    public DataConverterShulkerBoxItem() {
    }

    public int getDataVersion() {
        return 813;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:shulker_box".equals(cmp.getString("id")) && cmp.contains("tag", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");

            if (nbttagcompound1.contains("BlockEntityTag", 10)) {
                net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttagcompound1.getCompound("BlockEntityTag");

                if (nbttagcompound2.getList("Items", 10).isEmpty()) {
                    nbttagcompound2.remove("Items");
                }

                int i = nbttagcompound2.getInt("Color");

                nbttagcompound2.remove("Color");
                if (nbttagcompound2.isEmpty()) {
                    nbttagcompound1.remove("BlockEntityTag");
                }

                if (nbttagcompound1.isEmpty()) {
                    cmp.remove("tag");
                }

                cmp.putString("id", DataConverterShulkerBoxItem.BOX_TYPES[i % 16]);
            }
        }

        return cmp;
    }

}
