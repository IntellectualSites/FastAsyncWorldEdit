package com.boydti.fawe.beta;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.implementation.QueueHandler;
import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Flood {
    private final int maxBranch;
    private final int maxDepth;
    private final Direction[] directions;

    private int[] queue;
    private long[] visit;

    private int[][] queues;
    private long[][] visits;

    private int X, Y, Z;

    private ConcurrentLinkedQueue<int[]> queuePool = new ConcurrentLinkedQueue<>();
    private final Long2ObjectLinkedOpenHashMap<long[][]> chunkVisits;
    private final Long2ObjectLinkedOpenHashMap<int[][]> chunkQueues;

    public Flood(int maxBranch, int maxDepth, Direction[] directions) {
        this.maxBranch = maxBranch;
        this.maxDepth = maxDepth;
        this.directions = directions;

        this.queues = new int[27][];
        this.visits = new long[27][];

        this.chunkVisits = new Long2ObjectLinkedOpenHashMap<>();
        this.chunkQueues = new Long2ObjectLinkedOpenHashMap<>();
    }

    public synchronized void run(World world) {
        QueueHandler queueHandler = Fawe.get().getQueueHandler();
        IQueueExtent fq = queueHandler.getQueue(world);
        while (!chunkQueues.isEmpty()) {
            long firstKey = chunkQueues.firstLongKey();
            int X = MathMan.unpairIntX(firstKey);
            int Z = MathMan.unpairIntY(firstKey);
            int[][] chunkQueue = chunkQueues.get(firstKey);
            // apply
        }
    }

    private void init(int X, int Y, int Z) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
    }

    public void start(int x, int y, int z) {
        push(x, y, z, 0);
    }

    private void push(int x, int y, int z, int depth) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = MathMan.pairInt(X, Z);
        int layer = y >> 4;
        int[] section = getOrCreateQueue(pair, layer);
        int val = (x & 15) + ((z & 15) << 4)  + ((y & 15) << 8) + (depth << 12);
        push(section, val);
    }

    private int[] getOrCreateQueue(long pair, int layer) {
        int[][] arrs = chunkQueues.get(pair);
        if (arrs == null) {
            chunkQueues.put(pair, arrs = new int[16][]);
        }
        int[] section = arrs[layer];
        if (section == null) {
            arrs[layer] = section = newQueue();
        }
        return section;
    }

    private int[] newQueue() {
        int[] arr = queuePool.poll();
        if (arr != null) {
            arr[0] = 2;
            arr[1] = 2;
            return arr;
        }
        return new int[4096];
    }

    public int poll() {
        int index = queue[0];
        if (index == queue[1]) {
            return -1;
        }
        queue[0] = index + 1;
        return queue[index];
    }

    private void push(int[] queue, int val) {
        int indexStart = queue[0];
        int indexEnd = queue[1];
        push(indexStart, indexEnd, queue, val);
    }

    private void push(int indexStart, int indexEnd, int[] queue, int val) {
        if (indexStart > 2) {
            queue[0] = --indexStart;
            queue[indexStart] = val;
        } else {
            queue[indexEnd] = val;
            queue[0] = ++indexEnd;
        }
    }

    public Direction[] getDirections() {
        return directions;
    }

    public int getMaxBranch() {
        return maxBranch;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void apply(int x, int y, int z, int depth) {
        for (int i = 0, j = 0; i < directions.length && j < maxBranch; i++) {
            final Direction dir = directions[i];
            final int ty = y + dir.getBlockY();
            final int tx = x + dir.getBlockX();
            final int tz = z + dir.getBlockZ();

            int index;
            long[] visit;
            int[] queue;
            final int or = tx | ty | tz;
            if (or > 15 || or < 0) {
                visit = this.visit;
                queue = this.queue;
                index = tx + (tz << 4) + (ty << 8);
            } else {
                int nextX = tx >> 4;
                int nextY = ty >> 4;
                int nextZ = tz >> 4;
                int sectionIndex = nextX + nextZ * 3 + nextZ * 9 + 13;
                visit = visits[sectionIndex];
                queue = queues[sectionIndex];
                if (visit == null || queue == null) {
                    long pair = MathMan.pairInt(X + nextX, Z + nextZ);
                    int layer = Y + nextY;
                    if (layer < 0 || layer > 15) {
                        continue;
                    }
                    queues[sectionIndex] = queue = getOrCreateQueue(pair, layer);
                }
                index = (tx & 15) + ((tz & 15) << 4) + ((ty & 15) << 8);
            }
            if (!getAndSet(visit, index)) {
                j++;
                push(queue, index + (depth << 12));
            }
        }
    }

    public void set(long[] bits, int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public final boolean getAndSet(long[] bits, int i) {
        int index = i >> 6;
        long offset = (1L << (i & 0x3F));
        long val = bits[index];
        if ((val & offset) != 0) {
            return true;
        } else {
            bits[index] |= offset;
            return false;
        }
    }

    public boolean get(long[] bits, final int i) {
        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
    }
}
