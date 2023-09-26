package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorStructure implements DataInspector {

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        net.minecraft.nbt.ListTag nbttaglist;
        int j;
        net.minecraft.nbt.CompoundTag nbttagcompound1;

        if (cmp.contains("entities", 9)) {
            nbttaglist = cmp.getList("entities", 10);

            for (j = 0; j < nbttaglist.size(); ++j) {
                nbttagcompound1 = (net.minecraft.nbt.CompoundTag) nbttaglist.get(j);
                if (nbttagcompound1.contains("nbt", 10)) {
                    PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.ENTITY, nbttagcompound1,
                            "nbt", sourceVer, targetVer);
                }
            }
        }

        if (cmp.contains("blocks", 9)) {
            nbttaglist = cmp.getList("blocks", 10);

            for (j = 0; j < nbttaglist.size(); ++j) {
                nbttagcompound1 = (net.minecraft.nbt.CompoundTag) nbttaglist.get(j);
                if (nbttagcompound1.contains("nbt", 10)) {
                    PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.BLOCK_ENTITY, nbttagcompound1, "nbt", sourceVer, targetVer);
                }
            }
        }

        return cmp;
    }

}
