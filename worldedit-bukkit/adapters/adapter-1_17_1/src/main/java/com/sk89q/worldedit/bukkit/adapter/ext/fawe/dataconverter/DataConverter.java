package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public interface DataConverter {

    int getDataVersion();

    net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp);

}
