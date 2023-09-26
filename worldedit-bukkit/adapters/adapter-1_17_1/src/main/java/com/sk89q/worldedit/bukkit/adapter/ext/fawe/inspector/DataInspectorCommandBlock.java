package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.PaperweightOldIdMapper;
import net.minecraft.resources.ResourceLocation;

public class DataInspectorCommandBlock implements DataInspector {

    ResourceLocation tileEntityCommand = PaperweightOldIdMapper.getKey("TileEntityCommand");

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (tileEntityCommand.equals(new ResourceLocation(cmp.getString("id")))) {
            cmp.putString("id", "Control");
            PaperweightDataConverters.convert(PaperweightDataConverters.LegacyType.BLOCK_ENTITY, cmp, sourceVer, targetVer);
            cmp.putString("id", "MinecartCommandBlock");
        }

        return cmp;
    }

}
