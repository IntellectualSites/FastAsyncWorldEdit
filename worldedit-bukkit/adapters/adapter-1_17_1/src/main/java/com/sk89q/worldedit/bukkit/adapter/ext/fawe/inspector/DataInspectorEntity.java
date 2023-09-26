package com.sk89q.worldedit.bukkit.adapter.ext.fawe.inspector;

import com.sk89q.worldedit.bukkit.adapter.ext.fawe.PaperweightDataConverters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataInspectorEntity implements DataInspector {

    private static final Logger a = LogManager.getLogger(PaperweightDataConverters.class);

    public DataInspectorEntity() {
    }

    public net.minecraft.nbt.CompoundTag inspect(net.minecraft.nbt.CompoundTag cmp, int sourceVer, int targetVer) {
        net.minecraft.nbt.CompoundTag nbttagcompound1 = cmp.getCompound("tag");

        if (nbttagcompound1.contains("EntityTag", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound2 = nbttagcompound1.getCompound("EntityTag");
            String s = cmp.getString("id");
            String s1;

            if ("minecraft:armor_stand".equals(s)) {
                s1 = sourceVer < 515 ? "ArmorStand" : "minecraft:armor_stand";
            } else {
                if (!"minecraft:spawn_egg".equals(s)) {
                    return cmp;
                }

                s1 = nbttagcompound2.getString("id");
            }

            boolean flag;

            flag = !nbttagcompound2.contains("id", 8);
            nbttagcompound2.putString("id", s1);

            PaperweightDataConverters.convert(PaperweightDataConverters.LegacyType.ENTITY, nbttagcompound2, sourceVer, targetVer);
            if (flag) {
                nbttagcompound2.remove("id");
            }
        }

        return cmp;
    }

}
