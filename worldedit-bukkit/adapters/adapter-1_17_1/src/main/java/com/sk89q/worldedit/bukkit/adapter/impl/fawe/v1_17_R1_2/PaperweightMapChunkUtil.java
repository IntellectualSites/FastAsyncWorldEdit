package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2;

import com.fastasyncworldedit.bukkit.adapter.MapChunkUtil;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;

public class PaperweightMapChunkUtil extends MapChunkUtil<ClientboundLevelChunkPacket> {

    public PaperweightMapChunkUtil() throws NoSuchFieldException {
        fieldX = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("TWO_MEGABYTES", "a"));
        fieldZ = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("x", "b"));
        fieldBitMask = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("z", "c"));
        fieldHeightMap = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("availableSections", "d"));
        fieldChunkData = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("biomes", "f"));
        fieldBlockEntities = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("buffer", "g"));
        fieldFull = ClientboundLevelChunkPacket.class.getDeclaredField(Refraction.pickName("blockEntitiesTags", "h"));
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
