package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterBanner implements DataConverter {

    public DataConverterBanner() {
    }

    public int getDataVersion() {
        return 804;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("minecraft:banner".equals(cmp.getString("id")) && cmp.contains("tag", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");

            if (nbttagcompound1.contains("BlockEntityTag", 10)) {
                net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttagcompound1.getCompound("BlockEntityTag");

                if (nbttagcompound2.contains("Base", 99)) {
                    cmp.putShort("Damage", (short) (nbttagcompound2.getShort("Base") & 15));
                    if (nbttagcompound1.contains("display", 10)) {
                        net.minecraft.nbt.CompoundTag nbttagcompound3 = nbttagcompound1.getCompound("display");

                        if (nbttagcompound3.contains("Lore", 9)) {
                            net.minecraft.nbt.ListTag nbttaglist = nbttagcompound3.getList("Lore", 8);

                            if (nbttaglist.size() == 1 && "(+NBT)".equals(nbttaglist.getString(0))) {
                                return cmp;
                            }
                        }
                    }

                    nbttagcompound2.remove("Base");
                    if (nbttagcompound2.isEmpty()) {
                        nbttagcompound1.remove("BlockEntityTag");
                    }

                    if (nbttagcompound1.isEmpty()) {
                        cmp.remove("tag");
                    }
                }
            }
        }

        return cmp;
    }

}
