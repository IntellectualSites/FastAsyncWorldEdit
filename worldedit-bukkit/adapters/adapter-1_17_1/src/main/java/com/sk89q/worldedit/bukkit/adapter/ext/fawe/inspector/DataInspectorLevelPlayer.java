package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorLevelPlayer implements DataInspector {

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (cmp.contains("Player", 10)) {
            PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.PLAYER, cmp, "Player", sourceVer,
                    targetVer);
        }

        return cmp;
    }

}
