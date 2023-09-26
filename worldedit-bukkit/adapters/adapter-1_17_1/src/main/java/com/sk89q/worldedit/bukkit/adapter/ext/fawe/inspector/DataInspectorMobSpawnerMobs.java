package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.PaperweightOldIdMapper;
import net.minecraft.resources.ResourceLocation;

public class DataInspectorMobSpawnerMobs implements DataInspector {

    ResourceLocation tileEntityMobSpawner = PaperweightOldIdMapper.getKey("TileEntityMobSpawner");

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (tileEntityMobSpawner.equals(new ResourceLocation(cmp.getString("id")))) {
            if (cmp.contains("SpawnPotentials", 9)) {
                net.minecraft.nbt.ListTag nbttaglist = cmp.getList("SpawnPotentials", 10);

                for (int j = 0; j < nbttaglist.size(); ++j) {
                    net.minecraft.nbt.CompoundTag nbttagcompound1 = nbttaglist.getCompound(j);

                    PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.ENTITY, nbttagcompound1,
                            "Entity", sourceVer, targetVer);
                }
            }

            PaperweightDataConverters.convertCompound(PaperweightDataConverters.LegacyType.ENTITY, cmp, "SpawnData", sourceVer,
                    targetVer);
        }

        return cmp;
    }

}
