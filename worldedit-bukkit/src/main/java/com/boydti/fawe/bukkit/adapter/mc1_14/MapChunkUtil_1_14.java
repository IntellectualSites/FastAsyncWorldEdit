package com.boydti.fawe.bukkit.adapter.mc1_14;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.ChunkSection;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.NBTBase;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class MapChunkUtil_1_14 {
    private static final Field fieldX;

    private static final Field fieldZ;

    private static final Field fieldHeightMap;

    private static final Field fieldBitMask;

    private static final Field fieldChunkData;

    private static final Field fieldBlockEntities;

    private static final Field fieldFull;


    static {
        try {
            fieldX = PacketPlayOutMapChunk.class.getDeclaredField("a");
            fieldZ = PacketPlayOutMapChunk.class.getDeclaredField("b");
            fieldBitMask = PacketPlayOutMapChunk.class.getDeclaredField("c");
            fieldHeightMap = PacketPlayOutMapChunk.class.getDeclaredField("d");
            fieldChunkData = PacketPlayOutMapChunk.class.getDeclaredField("e");
            fieldBlockEntities = PacketPlayOutMapChunk.class.getDeclaredField("f");
            fieldFull = PacketPlayOutMapChunk.class.getDeclaredField("g");

            fieldX.setAccessible(true);
            fieldZ.setAccessible(true);
            fieldBitMask.setAccessible(true);
            fieldHeightMap.setAccessible(true);
            fieldChunkData.setAccessible(true);
            fieldBlockEntities.setAccessible(true);
            fieldFull.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public static PacketPlayOutMapChunk create(WorldServer world, BukkitImplAdapter<NBTBase> adapter, ChunkPacket packet) {
        IBlocks chunk = packet.getChunk();
        try {
            PacketPlayOutMapChunk nmsPacket;
            int bitMask = packet.getChunk().getBitMask();
            if (bitMask == 0) {
                nmsPacket = Fawe.get().getQueueHandler().sync((Callable<PacketPlayOutMapChunk>) () -> {
                    Chunk nmsChunk = world.getChunkAt(packet.getChunkX(), packet.getChunkZ());
                    PacketPlayOutMapChunk nmsPacket1 = new PacketPlayOutMapChunk(nmsChunk, 65535);
                    byte[] data = (byte[]) fieldChunkData.get(nmsPacket1);
                    int len = data.length;

                    ByteBuffer buffer = ByteBuffer.wrap(data, len - 256 * 4, 256 * 4);
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BiomeType biome = chunk.getBiomeType(x, z);
                            if (biome != null) {
                                buffer.putInt(biome.getLegacyId());
                            } else {
                                buffer.getInt();
                            }
                        }
                    }

                    return nmsPacket1;
                }).get();
            } else {
                nmsPacket = new PacketPlayOutMapChunk();
                fieldX.setInt(nmsPacket, packet.getChunkX());
                fieldZ.setInt(nmsPacket, packet.getChunkZ());
                fieldBitMask.set(nmsPacket, packet.getChunk().getBitMask());
                NBTBase heightMap = adapter.fromNative(/* packet.getHeightMap() */ new CompoundTag(new HashMap<>()));
                fieldHeightMap.set(nmsPacket, heightMap);

                fieldChunkData.set(nmsPacket, packet.getSectionBytes());

                Map<BlockVector3, CompoundTag> tiles = packet.getChunk().getTiles();
                ArrayList<NBTTagCompound> nmsTiles = new ArrayList<>(tiles.size());
                for (Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                    NBTBase nmsTag = adapter.fromNative(entry.getValue());
                    nmsTiles.add((NBTTagCompound) nmsTag);
                }
                fieldBlockEntities.set(nmsPacket, nmsTiles);
                fieldFull.set(nmsPacket, packet.isFull());
            }
            return nmsPacket;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
