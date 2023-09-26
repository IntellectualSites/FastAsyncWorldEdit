package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.PaperweightOldIdMapper;
import net.minecraft.resources.ResourceLocation;

public abstract class DataInspectorTagged implements DataInspector {

    private final ResourceLocation key;

    DataInspectorTagged(String type) {
        this.key = PaperweightOldIdMapper.getKey(type);
    }

    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (this.key.equals(new ResourceLocation(cmp.getString("id")))) {
            cmp = this.inspectChecked(cmp, sourceVer, targetVer);
        }

        return cmp;
    }

    abstract net.minecraft.nbt.CompoundTag inspectChecked(
            net.minecraft.nbt.CompoundTag nbttagcompound,
            int sourceVer,
            int targetVer
    );

}
