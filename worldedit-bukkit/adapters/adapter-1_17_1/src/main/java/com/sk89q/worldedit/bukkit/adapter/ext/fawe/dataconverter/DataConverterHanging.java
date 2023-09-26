package com.sk89q.worldedit.bukkit.adapter.ext.fawe.dataconverter;

import net.minecraft.core.Direction;

public class DataConverterHanging implements DataConverter {

    public DataConverterHanging() {
    }

    public int getDataVersion() {
        return 111;
    }

    public net.minecraft.nbt.CompoundTag convert(net.minecraft.nbt.CompoundTag cmp) {
        String s = cmp.getString("id");
        boolean flag = "Painting".equals(s);
        boolean flag1 = "ItemFrame".equals(s);

        if ((flag || flag1) && !cmp.contains("Facing", 99)) {
            Direction enumdirection;

            if (cmp.contains("Direction", 99)) {
                enumdirection = Direction.from2DDataValue(cmp.getByte("Direction"));
                cmp.putInt("TileX", cmp.getInt("TileX") + enumdirection.getStepX());
                cmp.putInt("TileY", cmp.getInt("TileY") + enumdirection.getStepY());
                cmp.putInt("TileZ", cmp.getInt("TileZ") + enumdirection.getStepZ());
                cmp.remove("Direction");
                if (flag1 && cmp.contains("ItemRotation", 99)) {
                    cmp.putByte("ItemRotation", (byte) (cmp.getByte("ItemRotation") * 2));
                }
            } else {
                enumdirection = Direction.from2DDataValue(cmp.getByte("Dir"));
                cmp.remove("Dir");
            }

            cmp.putByte("Facing", (byte) enumdirection.get2DDataValue());
        }

        return cmp;
    }

}
