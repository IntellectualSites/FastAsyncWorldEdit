package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterSkeleton implements DataConverter {

    public DataConverterSkeleton() {
    }

    public int getDataVersion() {
        return 701;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        String s = cmp.getString("id");

        if ("Skeleton".equals(s)) {
            int i = cmp.getInt("SkeletonType");

            if (i == 1) {
                cmp.putString("id", "WitherSkeleton");
            } else if (i == 2) {
                cmp.putString("id", "Stray");
            }

            cmp.remove("SkeletonType");
        }

        return cmp;
    }

}
