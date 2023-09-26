package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorPlayer implements DataInspector {

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        PaperweightDataConverters.convertItems(cmp, "Inventory", sourceVer, targetVer);
        PaperweightDataConverters.convertItems(cmp, "EnderItems", sourceVer, targetVer);
        if (cmp.contains("ShoulderEntityLeft", 10)) {
            PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.ENTITY, cmp, "ShoulderEntityLeft", sourceVer, targetVer);
        }

        if (cmp.contains("ShoulderEntityRight", 10)) {
            PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.ENTITY, cmp, "ShoulderEntityRight", sourceVer, targetVer);
        }

        return cmp;
    }

}
