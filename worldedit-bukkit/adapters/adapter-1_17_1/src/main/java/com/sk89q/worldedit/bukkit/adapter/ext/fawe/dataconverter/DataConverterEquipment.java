package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public final class DataConverterEquipment implements DataConverter {

    public DataConverterEquipment() {
    }

    public int getDataVersion() {
        return 100;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        net.minecraft.nbt.ListTag nbttaglist = cmp.getList("Equipment", 10);
        net.minecraft.nbt.ListTag nbttaglist1;

        if (!nbttaglist.isEmpty() && !cmp.contains("HandItems", 10)) {
            nbttaglist1 = new net.minecraft.nbt.ListTag();
            nbttaglist1.add(nbttaglist.get(0));
            nbttaglist1.add(new net.minecraft.nbt.CompoundTag());
            cmp.put("HandItems", nbttaglist1);
        }

        if (nbttaglist.size() > 1 && !cmp.contains("ArmorItem", 10)) {
            nbttaglist1 = new net.minecraft.nbt.ListTag();
            nbttaglist1.add(nbttaglist.get(1));
            nbttaglist1.add(nbttaglist.get(2));
            nbttaglist1.add(nbttaglist.get(3));
            nbttaglist1.add(nbttaglist.get(4));
            cmp.put("ArmorItems", nbttaglist1);
        }

        cmp.remove("Equipment");
        if (cmp.contains("DropChances", 9)) {
            nbttaglist1 = cmp.getList("DropChances", 5);
            net.minecraft.nbt.ListTag nbttaglist2;

            if (!cmp.contains("HandDropChances", 10)) {
                nbttaglist2 = new net.minecraft.nbt.ListTag();
                nbttaglist2.add(net.minecraft.nbt.FloatTag.valueOf(nbttaglist1.getFloat(0)));
                nbttaglist2.add(net.minecraft.nbt.FloatTag.valueOf(0.0F));
                cmp.put("HandDropChances", nbttaglist2);
            }

            if (!cmp.contains("ArmorDropChances", 10)) {
                nbttaglist2 = new net.minecraft.nbt.ListTag();
                nbttaglist2.add(net.minecraft.nbt.FloatTag.valueOf(nbttaglist1.getFloat(1)));
                nbttaglist2.add(net.minecraft.nbt.FloatTag.valueOf(nbttaglist1.getFloat(2)));
                nbttaglist2.add(net.minecraft.nbt.FloatTag.valueOf(nbttaglist1.getFloat(3)));
                nbttaglist2.add(net.minecraft.nbt.FloatTag.valueOf(nbttaglist1.getFloat(4)));
                cmp.put("ArmorDropChances", nbttaglist2);
            }

            cmp.remove("DropChances");
        }

        return cmp;
    }

}
