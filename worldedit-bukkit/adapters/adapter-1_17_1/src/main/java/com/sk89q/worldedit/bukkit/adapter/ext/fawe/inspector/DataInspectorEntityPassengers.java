package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorEntityPassengers implements DataInspector {

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (cmp.contains("Passengers", 9)) {
            net.minecraft.nbt.ListTag nbttaglist = cmp.getList("Passengers", 10);

            for (int j = 0; j < nbttaglist.size(); ++j) {
                nbttaglist.set(j, PaperweightDataConverters.convert(PaperweightDataConverters.LegacyType.ENTITY,
                        nbttaglist.getCompound(j), sourceVer, targetVer));
            }
        }

        return cmp;
    }

}
