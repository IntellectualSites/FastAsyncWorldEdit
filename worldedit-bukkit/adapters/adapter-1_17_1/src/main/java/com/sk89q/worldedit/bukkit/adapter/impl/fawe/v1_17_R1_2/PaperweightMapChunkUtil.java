package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2;

import com.fastasyncworldedit.bukkit.adapter.MapChunkUtil;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;

public class PaperweightMapChunkUtil extends MapChunkUtil<ClientboundLevelChunkPacket> {

    public PaperweightMapChunkUtil() throws NoSuchFieldException {
        fieldX = ClientboundLevelChunkPacket.class.getDeclaredField("TWO_MEGABYTES");
        fieldZ = ClientboundLevelChunkPacket.class.getDeclaredField("x");
        fieldBitMask = ClientboundLevelChunkPacket.class.getDeclaredField("z");
        fieldHeightMap = ClientboundLevelChunkPacket.class.getDeclaredField("availableSections");
        fieldChunkData = ClientboundLevelChunkPacket.class.getDeclaredField("biomes");
        fieldBlockEntities = ClientboundLevelChunkPacket.class.getDeclaredField("buffer");
        fieldFull = ClientboundLevelChunkPacket.class.getDeclaredField("blockEntitiesTags");
        fieldX.setAccessible(true);
        fieldZ.setAccessible(true);
        fieldBitMask.setAccessible(true);
        fieldHeightMap.setAccessible(true);
        fieldChunkData.setAccessible(true);
        fieldBlockEntities.setAccessible(true);
        fieldFull.setAccessible(true);
    }

    @Override
    public ClientboundLevelChunkPacket createPacket() {
        // TODO ??? return new ClientboundLevelChunkPacket();
        throw new UnsupportedOperationException();
    }

}
