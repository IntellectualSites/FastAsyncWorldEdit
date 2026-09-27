package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_5;

import com.fastasyncworldedit.bukkit.adapter.MapChunkUtil;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

//TODO un-very-break-this
public class PaperweightMapChunkUtil extends MapChunkUtil<ClientboundLevelChunkWithLightPacket> {

    public PaperweightMapChunkUtil() throws NoSuchFieldException {
        fieldX = ClientboundLevelChunkPacketData.class.getDeclaredField("TWO_MEGABYTES");
        fieldZ = ClientboundLevelChunkWithLightPacket.class.getDeclaredField("x");
        fieldBitMask = ClientboundLevelChunkWithLightPacket.class.getDeclaredField("z");
        fieldHeightMap = ClientboundLevelChunkPacketData.class.getDeclaredField("heightmaps");
        fieldChunkData = ClientboundLevelChunkWithLightPacket.class.getDeclaredField("chunkData");
        fieldBlockEntities = ClientboundLevelChunkPacketData.class.getDeclaredField("buffer");
        fieldFull = ClientboundLevelChunkPacketData.class.getDeclaredField("blockEntitiesData");
        fieldX.setAccessible(true);
        fieldZ.setAccessible(true);
        fieldBitMask.setAccessible(true);
        fieldHeightMap.setAccessible(true);
        fieldChunkData.setAccessible(true);
        fieldBlockEntities.setAccessible(true);
        fieldFull.setAccessible(true);
    }

    @Override
    public ClientboundLevelChunkWithLightPacket createPacket() {
        // TODO ??? return new ClientboundLevelChunkPacket();
        throw new UnsupportedOperationException();
    }

}
