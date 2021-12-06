package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_18_R1;

import com.fastasyncworldedit.bukkit.adapter.MapChunkUtil;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

//TODO un-very-break-this
public class PaperweightMapChunkUtil extends MapChunkUtil<ClientboundLevelChunkWithLightPacket> {

    public PaperweightMapChunkUtil() throws NoSuchFieldException {
        fieldX = ClientboundLevelChunkPacketData.class.getDeclaredField(Refraction.pickName("TWO_MEGABYTES", "a"));
        fieldZ = ClientboundLevelChunkWithLightPacket.class.getDeclaredField(Refraction.pickName("x", "b"));
        fieldBitMask = ClientboundLevelChunkWithLightPacket.class.getDeclaredField(Refraction.pickName("z", "c"));
        fieldChunkData = ClientboundLevelChunkPacketData.class.getDeclaredField(Refraction.pickName("biomes", "f"));
        fieldBlockEntities = ClientboundLevelChunkPacketData.class.getDeclaredField(Refraction.pickName("buffer", "g"));
        fieldFull = ClientboundLevelChunkPacketData.class.getDeclaredField(Refraction.pickName("blockEntitiesTags", "h"));
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
