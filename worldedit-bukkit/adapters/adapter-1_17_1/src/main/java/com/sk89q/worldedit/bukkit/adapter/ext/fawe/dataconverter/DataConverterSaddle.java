package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterSaddle implements DataConverter {

    public DataConverterSaddle() {
    }

    public int getDataVersion() {
        return 110;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        if ("EntityHorse".equals(cmp.getString("id")) && !cmp.contains("SaddleItem", 10) && cmp.getBoolean("Saddle")) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = new net.minecraft.nbt.CompoundTag();

            nbttagcompound1.putString("id", "minecraft:saddle");
            nbttagcompound1.putByte("Count", (byte) 1);
            nbttagcompound1.putShort("Damage", (short) 0);
            cmp.put("SaddleItem", nbttagcompound1);
            cmp.remove("Saddle");
        }

        return cmp;
    }

}
