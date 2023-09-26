package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.PaperweightOldIdMapper;
import net.minecraft.resources.ResourceLocation;

public class DataInspectorMobSpawnerMinecart implements DataInspector {

    ResourceLocation entityMinecartMobSpawner = PaperweightOldIdMapper.getKey("EntityMinecartMobSpawner");
    ResourceLocation tileEntityMobSpawner = PaperweightOldIdMapper.getKey("TileEntityMobSpawner");

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        String s = cmp.getString("id");
        if (entityMinecartMobSpawner.equals(new ResourceLocation(s))) {
            cmp.putString("id", tileEntityMobSpawner.toString());
            PaperweightDataConverters.convert(PaperweightDataConverters.LegacyType.BLOCK_ENTITY, cmp, sourceVer, targetVer);
            cmp.putString("id", s);
        }

        return cmp;
    }

}
