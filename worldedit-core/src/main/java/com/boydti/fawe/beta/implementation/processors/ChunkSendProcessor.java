package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class ChunkSendProcessor implements IBatchProcessor {
    private final Supplier<Stream<Player>> players;
    private final World world;

    public ChunkSendProcessor(World world, Supplier<Stream<Player>> players) {
        this.players = players;
        this.world = world;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        boolean replaceAll = true;
        ChunkPacket packet = new ChunkPacket(chunkX, chunkZ, () -> set, replaceAll);
        Stream<Player> stream = this.players.get();
        if (stream == null) {
            world.sendFakeChunk(null, packet);
        } else {
            stream.filter(player -> player.getWorld().equals(world))
                .forEach(player -> world.sendFakeChunk(player, packet));
        }
        return set;
    }

    @Override
    public Extent construct(Extent child) {
        return null;
    }
}
