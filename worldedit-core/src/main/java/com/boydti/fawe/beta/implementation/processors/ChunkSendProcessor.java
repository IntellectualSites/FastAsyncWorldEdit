package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.CombinedBlocks;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

import java.util.Collection;
import java.util.function.Supplier;

public class ChunkSendProcessor implements IBatchProcessor {
    private final Supplier<Collection<Player>> players;
    private final World world;
    private final boolean full;

    public ChunkSendProcessor(World world, Supplier<Collection<Player>> players) {
        this(world, players, false);
    }

    public ChunkSendProcessor(World world, Supplier<Collection<Player>> players, boolean full) {
        this.players = players;
        this.world = world;
        this.full = full;
    }

    public World getWorld() {
        return world;
    }

    public Supplier<Collection<Player>> getPlayers() {
        return players;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        IBlocks blocks;
        boolean full = this.full;
        if (full) {
            blocks = set;
        } else {
            blocks = combine(chunk, get, set);
            if (set.hasBiomes()) {
                full = true;
            }
        }
        ChunkPacket packet = new ChunkPacket(chunkX, chunkZ, () -> blocks, full);
        Collection<Player> stream = this.players.get();
        if (stream == null) {
            world.sendFakeChunk(null, packet);
        } else {
            for (Player player : stream) {
                if (player.getWorld().equals(world)) {
                    world.sendFakeChunk(player, packet);
                }
            }
        }
        return set;
    }

    public IBlocks combine(IChunk chunk, IChunkGet get, IChunkSet set) {
        return new CombinedBlocks(get, set, 0);
    }

    @Override
    public Extent construct(Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }
}