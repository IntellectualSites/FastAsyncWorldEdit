package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.IntFaweChunk;
import com.boydti.fawe.example.NullQueueIntFaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class VisualExtent extends AbstractDelegateExtent {

    private final FaweQueue queue;
    private Long2ObjectMap<VisualChunk> chunks = new Long2ObjectOpenHashMap<>();

    public VisualExtent(Extent parent, FaweQueue queue) {
        super(parent);
        this.queue = queue;
    }

    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    public VisualChunk getChunk(int cx, int cz) {
        return chunks.get(MathMan.pairInt(cx, cz));
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        BlockStateHolder previous = super.getLazyBlock(x, y, z);
        int cx = x >> 4;
        int cz = z >> 4;
        long chunkPair = MathMan.pairInt(cx, cz);
        VisualChunk chunk = chunks.get(chunkPair);
        if (previous.equals(block)) {
            if (chunk != null) {
                chunk.unset(x, y, z);
            }
            return false;
        } else {
            if (chunk == null) {
                chunk = new VisualChunk(cx, cz);
                chunks.put(chunkPair, chunk);
            }
            chunk.setBlock(x, y, z, block.getInternalId());
            return true;
        }
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        // Do nothing
        return false;
    }

    public void clear(VisualExtent other, FawePlayer... players) {
        ObjectIterator<Long2ObjectMap.Entry<VisualChunk>> iter = chunks.long2ObjectEntrySet().iterator();
        while (iter.hasNext()) {
            Long2ObjectMap.Entry<VisualChunk> entry = iter.next();
            long pair = entry.getLongKey();
            int cx = MathMan.unpairIntX(pair);
            int cz = MathMan.unpairIntY(pair);
            VisualChunk chunk = entry.getValue();
            final VisualChunk otherChunk = other != null ? other.getChunk(cx, cz) : null;
            final IntFaweChunk newChunk = new NullQueueIntFaweChunk(cx, cz);
            final int bx = cx << 4;
            final int bz = cz << 4;
            if (otherChunk == null) {
                chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                    @Override
                    public void run(int localX, int y, int localZ, int combined) {
                        combined = queue.getCombinedId4Data(bx + localX, y, bz + localZ, 0);
                        newChunk.setBlock(localX, y, localZ, combined);
                    }
                });
            } else {
                chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                    @Override
                    public void run(int localX, int y, int localZ, int combined) {
                        if (combined != otherChunk.getBlockCombinedId(localX, y, localZ)) {
                            combined = queue.getCombinedId4Data(bx + localX, y, bz + localZ, 0);
                            newChunk.setBlock(localX, y, localZ, combined);
                        }
                    }
                });
            }
            if (newChunk.getTotalCount() != 0) {
                queue.sendBlockUpdate(newChunk, players);
            }
        }
    }

    public void visualize(FawePlayer players) {
        for (VisualChunk chunk : chunks.values()) {
            queue.sendBlockUpdate(chunk, players);
        }
    }
}
