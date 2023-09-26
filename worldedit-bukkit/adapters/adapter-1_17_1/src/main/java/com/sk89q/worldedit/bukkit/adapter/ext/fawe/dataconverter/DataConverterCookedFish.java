package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import net.minecraft.resources.ResourceLocation;

public class DataConverterCookedFish implements DataConverter {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation("cooked_fished");

    public DataConverterCookedFish() {
    }

    public int getDataVersion() {
        return 502;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if (cmp.contains("id", 8) && DataConverterCookedFish.RESOURCE_LOCATION.equals(new ResourceLocation(cmp.getString("id")))) {
            cmp.putString("id", "minecraft:cooked_fish");
        }

        return cmp;
    }

}
