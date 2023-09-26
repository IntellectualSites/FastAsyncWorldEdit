package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorItem extends DataInspectorTagged  {

    private final String[] keys;

    public DataInspectorItem(String oclass, String... astring) {
        super(oclass);
        this.keys = astring;
    }

    net.minecraft.nbt.CompoundTag inspectChecked(net.minecraft.nbt.CompoundTag nbttagcompound, int sourceVer, int targetVer) {
        for (String key : this.keys) {
            PaperweightDataConverters.convertItem(nbttagcompound, key, sourceVer, targetVer);
        }

        return nbttagcompound;
    }

}
