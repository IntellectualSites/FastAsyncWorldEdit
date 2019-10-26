package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.object.extent.NullExtent;
import com.google.common.base.Suppliers;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class ChunkSendProcessor extends AbstractDelegateExtent implements IBatchProcessor {
    private final Supplier<Stream<Player>> players;

    public ChunkSendProcessor(Supplier<Stream<Player>> players) {
        super(new NullExtent());
        this.players = players;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        boolean replaceAll = true;
        boolean sendBiome = set.getBiomes() != null;
        ChunkPacket packet = new ChunkPacket(chunkX, chunkZ, set, replaceAll, sendBiome);
        Supplier<byte[]> packetData = Suppliers.memoize(packet::get);
        players.get().forEach(plr -> plr.sendFakeChunk(chunkX, chunkZ, packetData));
        return set;
    }

    @Override
    public Extent construct(Extent child) {
        return null;
    }
}
