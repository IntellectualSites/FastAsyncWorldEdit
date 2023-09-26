package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

public class DataConverterRiding implements DataConverter {

    public DataConverterRiding() {
    }

    public int getDataVersion() {
        return 135;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        while (cmp.contains("Riding", 10)) {
            net.minecraft.nbt.CompoundTag nbttagcompound1 = this.b(cmp);

            this.convert(cmp, nbttagcompound1);
            cmp = nbttagcompound1;
        }

        return cmp;
    }

    protected void convert(net.minecraft.nbt.CompoundTag nbttagcompound, net.minecraft.nbt.CompoundTag nbttagcompound1) {
        net.minecraft.nbt.ListTag nbttaglist = new net.minecraft.nbt.ListTag();

        nbttaglist.add(nbttagcompound);
        nbttagcompound1.put("Passengers", nbttaglist);
    }

    protected net.minecraft.nbt.CompoundTag b(net.minecraft.nbt.CompoundTag nbttagcompound) {
        net.minecraft.nbt.CompoundTag nbttagcompound1 = nbttagcompound.getCompound("Riding");

        nbttagcompound.remove("Riding");
        return nbttagcompound1;
    }

}
