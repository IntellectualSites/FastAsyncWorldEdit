package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A chunk based search algorithm
 */
public class ScanChunk {
    private static final int MAX_QUEUE = 34816;
    public static final BlockVector3[] DEFAULT_DIRECTIONS = new BlockVector3[6];
    public static final BlockVector3[] DIAGONAL_DIRECTIONS;

    static {
        DEFAULT_DIRECTIONS[0] = (BlockVector3.at(0, -1, 0));
        DEFAULT_DIRECTIONS[1] = (BlockVector3.at(0, 1, 0));
        DEFAULT_DIRECTIONS[2] = (BlockVector3.at(-1, 0, 0));
        DEFAULT_DIRECTIONS[3] = (BlockVector3.at(1, 0, 0));
        DEFAULT_DIRECTIONS[4] = (BlockVector3.at(0, 0, -1));
        DEFAULT_DIRECTIONS[5] = (BlockVector3.at(0, 0, 1));
        List<BlockVector3> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        BlockVector3 pos = BlockVector3.at(x, y, z);
                        if (!list.contains(pos)) {
                            list.add(pos);
                        }
                    }
                }
            }
        }
        Collections.sort(list, new Comparator<BlockVector3>() {
            @Override
            public int compare(BlockVector3 o1, BlockVector3 o2) {
                return (int) Math.signum(o1.lengthSq() - o2.lengthSq());
            }
        });
        DIAGONAL_DIRECTIONS = list.toArray(new BlockVector3[list.size()]);
    }

    private final RegionFunction function;
    private final BlockVector3[] directions;
    private final Long2ObjectOpenHashMap<long[][]> visited;
    private final Long2ObjectOpenHashMap<char[]> queues;

    public ScanChunk(final RegionFunction function) {
        this.function = function;
        this.directions = DEFAULT_DIRECTIONS;

        this.queues = new Long2ObjectOpenHashMap<>();
        this.visited = new Long2ObjectOpenHashMap<>();
    }

    public static final long pairInt(int x, int y) {
        return (((long) x) << 32) | (y & 0xffffffffL);
    }

    public boolean isVisited(int x, int y, int z) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = pairInt(X, Z);
        long[][] chunk = visited.get(pair);
        if (chunk == null) return false;
        int layer = y >> 4;
        long[] section = chunk[layer];
        if (section == null) return false;
        return get(section, getLocalIndex(x & 15, y & 15, z & 15));
    }

    public void start(int x, int y, int z) {
        if (!isVisited(x, y, z)) {
            queue(x, y, z);
            visit(x, y, z);
        }
    }

    public void visit(int x, int y, int z) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = pairInt(X, Z);
        long[][] chunk = visited.get(pair);
        if (chunk == null) {
            visited.put(pair, chunk = new long[16][]);
        }
        int layer = y >> 4;
        long[] section = chunk[layer];
        if (section == null) {
            chunk[layer] = section = new long[64];
        }
        set(section, getLocalIndex(x & 15, y & 15, z & 15));
    }

    public void queue(int x, int y, int z) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = pairInt(X, Z);
        char[] queue = queues.get(pair);
        if (queue == null) {
            queue = queues.put(pair, queue = new char[MAX_QUEUE + 2]);
            queue[0] = 2;
            queue[1] = 2;
        }
        if (queue[1] >= queue.length) {
            queue[1] = 2;
        }
        queue[queue[1]++] = getLocalIndex(x & 15, y, z & 15);
    }

    public void process() {
        LongArraySet set = new LongArraySet();
        while (!queues.isEmpty()) {
            ObjectIterator<Long2ObjectMap.Entry<char[]>> iter = queues.long2ObjectEntrySet().fastIterator();
            Long2ObjectMap.Entry<char[]> entry = iter.next();
            long index = entry.getLongKey();
            int X = MathMan.unpairIntX(index);
            int Z = MathMan.unpairIntY(index);
        }
    }

    public void set(long[] bits, int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public boolean get(long[] bits, final int i) {
        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
    }

    public char getLocalIndex(int x, int y, int z) {
        return (char) (y + (x << 8) + (z << 12));
    }


}
