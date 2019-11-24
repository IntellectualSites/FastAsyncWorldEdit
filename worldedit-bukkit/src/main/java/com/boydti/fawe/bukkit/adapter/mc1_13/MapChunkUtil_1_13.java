package com.boydti.fawe.bukkit.adapter.mc1_13;

import com.boydti.fawe.bukkit.adapter.MapChunkUtil;
import net.minecraft.server.v1_13_R2.PacketPlayOutMapChunk;

public class MapChunkUtil_1_13 extends MapChunkUtil<PacketPlayOutMapChunk> {
    public MapChunkUtil_1_13() throws NoSuchFieldException {
        fieldX = PacketPlayOutMapChunk.class.getDeclaredField("a");
        fieldZ = PacketPlayOutMapChunk.class.getDeclaredField("b");
        fieldBitMask = PacketPlayOutMapChunk.class.getDeclaredField("c");
        fieldChunkData = PacketPlayOutMapChunk.class.getDeclaredField("d");
        fieldBlockEntities = PacketPlayOutMapChunk.class.getDeclaredField("e");
        fieldFull = PacketPlayOutMapChunk.class.getDeclaredField("f");

        fieldX.setAccessible(true);
        fieldZ.setAccessible(true);
        fieldBitMask.setAccessible(true);
        fieldChunkData.setAccessible(true);
        fieldBlockEntities.setAccessible(true);
        fieldFull.setAccessible(true);
    }

    @Override
    public PacketPlayOutMapChunk createPacket() {
        return new PacketPlayOutMapChunk();
    }
}
