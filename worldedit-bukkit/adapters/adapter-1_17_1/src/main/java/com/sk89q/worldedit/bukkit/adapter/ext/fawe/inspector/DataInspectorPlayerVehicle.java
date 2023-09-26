package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorPlayerVehicle implements DataInspector {

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (cmp.contains("RootVehicle", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("RootVehicle");

            if (nbttagcompound1.contains("Entity", 10)) {
                PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.ENTITY, nbttagcompound1, "Entity", sourceVer, targetVer);
            }
        }

        return cmp;
    }

}
