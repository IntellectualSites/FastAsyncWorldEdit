package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterMobSpawner implements DataConverter {

    public DataConverterMobSpawner() {
    }

    public int getDataVersion() {
        return 107;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if (!"MobSpawner".equals(cmp.getString("id"))) {
            return cmp;
        } else {
            if (cmp.contains("EntityId", 8)) {
                String s = cmp.getString("EntityId");
                net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("SpawnData");

                nbttagcompound1.putString("id", s.isEmpty() ? "Pig" : s);
                cmp.put("SpawnData", nbttagcompound1);
                cmp.remove("EntityId");
            }

            if (cmp.contains("SpawnPotentials", 9)) {
                net.minecraft.nbt.ListTag nbttaglist = cmp.getList("SpawnPotentials", 10);

                for (int i = 0; i < nbttaglist.size(); ++i) {
                    net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttaglist.getCompound(i);

                    if (nbttagcompound2.contains("Type", 8)) {
                        net.minecraft.nbt.CompoundTag nbttagcompound3 = nbttagcompound2.getCompound("Properties");

                        nbttagcompound3.putString("id", nbttagcompound2.getString("Type"));
                        nbttagcompound2.put("Entity", nbttagcompound3);
                        nbttagcompound2.remove("Type");
                        nbttagcompound2.remove("Properties");
                    }
                }
            }

            return cmp;
        }
    }

}
