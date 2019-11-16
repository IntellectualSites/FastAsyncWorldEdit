package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.CombinedBlocks;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class PersistentChunkSendProcessor extends ChunkSendProcessor {
    private final Long2ObjectLinkedOpenHashMap<Character> current;
    private final IQueueExtent queue;
    private Long2ObjectLinkedOpenHashMap<Character> previous;

    public PersistentChunkSendProcessor(IQueueExtent queue, World world, PersistentChunkSendProcessor previous, Supplier<Stream<Player>> players) {
        super(world, players);
        this.current = new Long2ObjectLinkedOpenHashMap<>();
        this.previous = previous.current;
        this.queue = queue;
    }

    @Override
    public IBlocks combine(IChunk chunk, IChunkGet get, IChunkSet set) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        long pair = MathMan.pairInt(chunkX, chunkZ);
        Character bitMask = (char) (set.hasBiomes() ? Character.MAX_VALUE : set.getBitMask());
        current.put(pair, bitMask);
        Character lastValue = previous.remove(pair);
        return new CombinedBlocks(get, set, lastValue == null ? 0 : lastValue);
    }

    public void flush() {
        clear(previous);
        previous = null;
    }

    public void clear() {
        clear(current);
        current.clear();
    }

    public void clear(Long2ObjectLinkedOpenHashMap<Character> current) {
        if (current != null && !current.isEmpty()) {
            Stream<Player> players = getPlayers().get();
            for (Long2ObjectMap.Entry<Character> entry : current.long2ObjectEntrySet()) {
                long pair = entry.getLongKey();
                int chunkX = MathMan.unpairIntX(pair);
                int chunkZ = MathMan.unpairIntY(pair);
                BlockVector2 pos = BlockVector2.at(chunkX, chunkZ);
                Supplier<IBlocks> chunk = () -> queue.getOrCreateChunk(pos.getX(), pos.getZ());
                ChunkPacket packet = new ChunkPacket(pos.getX(), pos.getZ(), chunk, true);
                char bitMask = entry.getValue();
                if (players == null) {
                    getWorld().sendFakeChunk(null, packet);
                } else {
                    players.forEach(player -> getWorld().sendFakeChunk(player, packet));
                }
            }
        }
    }
}
