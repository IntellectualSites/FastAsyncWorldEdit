package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterHorse implements DataConverter {

    public DataConverterHorse() {
    }

    public int getDataVersion() {
        return 703;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("EntityHorse".equals(cmp.getString("id"))) {
            int i = cmp.getInt("Type");

            switch (i) {
                case 0:
                default:
                    cmp.putString("id", "Horse");
                    break;

                case 1:
                    cmp.putString("id", "Donkey");
                    break;

                case 2:
                    cmp.putString("id", "Mule");
                    break;

                case 3:
                    cmp.putString("id", "ZombieHorse");
                    break;

                case 4:
                    cmp.putString("id", "SkeletonHorse");
            }

            cmp.remove("Type");
        }

        return cmp;
    }

}
