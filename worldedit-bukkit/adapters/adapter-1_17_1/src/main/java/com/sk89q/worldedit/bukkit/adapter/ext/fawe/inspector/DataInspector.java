package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

public interface DataInspector {

    net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer);

}
