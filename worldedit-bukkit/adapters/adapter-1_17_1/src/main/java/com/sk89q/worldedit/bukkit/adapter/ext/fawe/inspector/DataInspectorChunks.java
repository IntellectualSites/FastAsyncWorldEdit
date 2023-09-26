package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;

public class DataInspectorChunks implements DataInspector {

    @Override
    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        if (cmp.contains("Level", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("Level");
            net.minecraft.nbt.ListTag nbttaglist;
            int j;

            if (nbttagcompound1.contains("Entities", 9)) {
                nbttaglist = nbttagcompound1.getList("Entities", 10);

                for (j = 0; j < nbttaglist.size(); ++j) {
                    nbttaglist.set(
                            j,
                            PaperweightDataConverters.convert(
                                    PaperweightDataConverters.LegacyType.ENTITY,
                                    (net.minecraft.nbt.CompoundTag) nbttaglist.get(j),
                                    sourceVer,
                                    targetVer
                            )
                    );
                }
            }

            if (nbttagcompound1.contains("TileEntities", 9)) {
                nbttaglist = nbttagcompound1.getList("TileEntities", 10);

                for (j = 0; j < nbttaglist.size(); ++j) {
                    nbttaglist.set(
                            j,
                            PaperweightDataConverters.convert(
                                    PaperweightDataConverters.LegacyType.BLOCK_ENTITY,
                                    (net.minecraft.nbt.CompoundTag) nbttaglist.get(j),
                                    sourceVer,
                                    targetVer
                            )
                    );
                }
            }
        }

        return cmp;
    }

}
