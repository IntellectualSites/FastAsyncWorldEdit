package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterZombieType implements DataConverter {

    public DataConverterZombieType() {
    }

    public int getDataVersion() {
        return 702;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("Zombie".equals(cmp.getString("id"))) {
            int i = cmp.getInt("ZombieType");

            switch (i) {
                case 0:
                default:
                    break;

                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    cmp.putString("id", "ZombieVillager");
                    cmp.putInt("Profession", i - 1);
                    break;

                case 6:
                    cmp.putString("id", "Husk");
            }

            cmp.remove("ZombieType");
        }

        return cmp;
    }

}
