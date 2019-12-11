package com.boydti.fawe.bukkit.adapter;

import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

public abstract class MapChunkUtil<T> {
    protected Field fieldX;
    protected Field fieldZ;
    protected Field fieldHeightMap;
    protected Field fieldBitMask;
    protected Field fieldChunkData;
    protected Field fieldBlockEntities;
    protected Field fieldFull;

    public abstract T createPacket();

    public T create(BukkitImplAdapter adapter, ChunkPacket packet) {
        try {
            T nmsPacket;
            int bitMask = packet.getChunk().getBitMask();
            nmsPacket = createPacket();
            fieldX.setInt(nmsPacket, packet.getChunkX());
            fieldZ.setInt(nmsPacket, packet.getChunkZ());
            fieldBitMask.set(nmsPacket, packet.getChunk().getBitMask());

            if (fieldHeightMap != null) {
                Object heightMap = adapter.fromNative(packet.getHeightMap());
                fieldHeightMap.set(nmsPacket, heightMap);
            }

            fieldChunkData.set(nmsPacket, packet.getSectionBytes());

            Map<BlockVector3, CompoundTag> tiles = packet.getChunk().getTiles();
            ArrayList<Object> nmsTiles = new ArrayList<>(tiles.size());
            for (Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                Object nmsTag = adapter.fromNative(entry.getValue());
                nmsTiles.add(nmsTag);
            }
            fieldBlockEntities.set(nmsPacket, nmsTiles);
            fieldFull.set(nmsPacket, packet.isFull());
            return nmsPacket;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
