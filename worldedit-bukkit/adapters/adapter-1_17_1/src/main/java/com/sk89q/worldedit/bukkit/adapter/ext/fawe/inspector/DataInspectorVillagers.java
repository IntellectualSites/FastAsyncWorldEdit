package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter.PaperweightOldIdMapper;
import net.minecraft.resources.ResourceLocation;

public class DataInspectorVillagers implements DataInspector {

    ResourceLocation entityVillager = PaperweightOldIdMapper.getKey("EntityVillager");

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (entityVillager.equals(new ResourceLocation(cmp.getString("id"))) && cmp.contains("Offers", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("Offers");

            if (nbttagcompound1.contains("Recipes", 9)) {
                net.minecraft.nbt.ListTag nbttaglist = nbttagcompound1.getList("Recipes", 10);

                for (int j = 0; j < nbttaglist.size(); ++j) {
                    net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttaglist.getCompound(j);

                    PaperweightDataConverters.convertItem(nbttagcompound2, "buy", sourceVer, targetVer);
                    PaperweightDataConverters.convertItem(nbttagcompound2, "buyB", sourceVer, targetVer);
                    PaperweightDataConverters.convertItem(nbttagcompound2, "sell", sourceVer, targetVer);
                    nbttaglist.set(j, nbttagcompound2);
                }
            }
        }

        return cmp;
    }

}
