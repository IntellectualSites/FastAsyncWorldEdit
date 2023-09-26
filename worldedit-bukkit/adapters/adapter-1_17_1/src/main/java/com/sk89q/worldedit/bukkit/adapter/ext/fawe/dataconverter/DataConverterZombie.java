package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import java.util.Random;

public class DataConverterZombie implements DataConverter {
    private static final Random RANDOM = new Random();

    public DataConverterZombie() {
    }

    public int getDataVersion() {
        return 502;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("Zombie".equals(cmp.getString("id")) && cmp.getBoolean("IsVillager")) {
            if (!cmp.contains("ZombieType", 99)) {
                int i = -1;

                if (cmp.contains("VillagerProfession", 99)) {
                    try {
                        i = this.convert(cmp.getInt("VillagerProfession"));
                    } catch (RuntimeException runtimeexception) {
                        ;
                    }
                }

                if (i == -1) {
                    i = this.convert(DataConverterZombie.RANDOM.nextInt(6));
                }

                cmp.putInt("ZombieType", i);
            }

            cmp.remove("IsVillager");
        }

        return cmp;
    }

    private int convert(int i) {
        return i >= 0 && i < 6 ? i : -1;
    }

}
