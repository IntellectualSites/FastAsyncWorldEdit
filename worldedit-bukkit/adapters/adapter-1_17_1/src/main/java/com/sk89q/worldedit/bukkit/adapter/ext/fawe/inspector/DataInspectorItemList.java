package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorItemList extends DataInspectorTagged {
    private final String[] keys;

    public DataInspectorItemList(String oclass, String... astring) {
        super(oclass);
        this.keys = astring;
    }

    net.minecraft.nbt.CompoundTag inspectChecked(net.minecraft.nbt.CompoundTag nbttagcompound, int sourceVer, int targetVer) {
        for (String s : this.keys) {
            PaperweightDataConverters.convertItems(nbttagcompound, s, sourceVer, targetVer);
        }

        return nbttagcompound;
    }
}
