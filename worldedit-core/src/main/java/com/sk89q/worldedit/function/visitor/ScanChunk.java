package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private final Long2ObjectOpenHashMap<long[][]> visits;
    private final Long2ObjectOpenHashMap<char[][]> queues;

    public ScanChunk(final RegionFunction function) {
        this.function = function;
        this.directions = DEFAULT_DIRECTIONS;

        this.queues = new Long2ObjectOpenHashMap<>();
        this.visits = new Long2ObjectOpenHashMap<>();
    }

    public static final long pairInt(int x, int y) {
        return (((long) x) << 32) | (y & 0xffffffffL);
    }

    public boolean isVisited(int x, int y, int z) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = pairInt(X, Z);
        long[][] chunk = visits.get(pair);
        if (chunk == null) return false;
        int layer = y >> 4;
        long[] section = chunk[layer];
        if (section == null) return false;
        return get(section, getLocalIndex(x & 15, y & 15, z & 15));
    }

    public void start(int x, int y, int z) {
        if (!isVisited(x, y, z)) {
            push(x, y, z);
            visit(x, y, z);
        }
    }

    public void visit(int x, int y, int z) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = pairInt(X, Z);
        long[][] arrs = visits.get(pair);
        if (arrs == null) {
            visits.put(pair, arrs = new long[16][]);
        }
        int layer = y >> 4;
        long[] section = arrs[layer];
        if (section == null) {
            arrs[layer] = section = new long[64];
        }
        set(section, getLocalIndex(x & 15, y & 15, z & 15));
    }

    private char[] getOrCreateQueue(long pair, int layer) {
        char[][] arrs = queues.get(pair);
        if (arrs == null) {
            queues.put(pair, arrs = new char[16][]);
        }

        char[] section = arrs[layer];
        if (section == null) {
            arrs[layer] = section = newQueue();
        }
        return section;
    }

    private void push(int x, int y, int z) {
        int X = x >> 4;
        int Z = z >> 4;
        long pair = pairInt(X, Z);
        int layer = y >> 4;
        char[] section = getOrCreateQueue(pair, layer);
        push(section, x & 15, y & 15, z & 15);
    }

    private void push(char[] queue, int x, int y, int z) {
        char indexStart = queue[0];
        char indexEnd = queue[1];
        push(indexStart, indexEnd, queue, x, y, z);
    }

    private void push(char indexStart, char indexEnd, char[] queue, int x, int y, int z) {
        char index = getLocalIndex(x, y, z);
        if (indexStart > 2) {
            queue[0] = --indexStart;
            queue[indexStart] = index;
        } else {
            queue[indexEnd] = index;
            queue[0] = ++indexEnd;
        }
    }

    public void process() {
        LongArraySet set = new LongArraySet();
        while (!queues.isEmpty()) {
//            ObjectIterator<Long2ObjectMap.Entry<char[][]>> iter = queues.long2ObjectEntrySet().fastIterator();
//            Long2ObjectMap.Entry<char[][]> entry = iter.next();
//            long index = entry.getLongKey();
//            int X = MathMan.unpairIntX(index);
//            int Z = MathMan.unpairIntY(index);
//            // check that adjacent chunks aren;t being processed
//
//            char[] queue = entry.getValue();
//            long[][] visit = visits.get(index);
//            if (visit == null) {
//                visits.put(index, visit = new long[16][]);
//            }
        }
    }

    private ConcurrentLinkedQueue<char[]> queuePool = new ConcurrentLinkedQueue<>();

    private char[] newQueue() {
        char[] arr = queuePool.poll();
        if (arr != null) {
            arr[0] = 2;
            arr[1] = 2;
            return arr;
        }
        return new char[4096];
    }

    public void process4(int xx, int yy, int zz, char[] queue, long[] visit) {
        char index;
        while ((index = queue[0]) != queue[1]) {
            queue[0]++;

            char triple = queue[index];
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = index >> 8;

            int absX = xx + x;
            int absY = yy + y;
            int absZ = zz + z;

            apply(xx + x, yy + y, zz + z);

            int x1 = x, x2 = x;

            // find start of scan-line
            int i1 = index;
            while (true) {
                if (x1 < 0) {
                    // queue in west chunk
                    break;
                }
                if (get(visit, i1)) break;
                // visit
                set(visit, i1);

                i1--;
                x1--;
            }
            i1++;
            x1++;

            // find end of scan-line
            int i2 = index;
            while (true) {
                if (x2 > 15) {
                    // queue in east chunk
                    break;
                }
                if (get(visit, i2)) break;
                set(visit, i2);
                i2++;
                x2++;
            }
            i2--;
            x2--;

            // find start
        }
    }

    public void apply(int x, int y, int z) {

    }

    public void process4(int X, int Z, char[][] queues, long[][] visit) {
        int xx = X << 4;
        int zz = Z << 4;

        // TODO fetch instead of create
        final BlockVector3[] dirs = directions;
        char[][] dirQueues = new char[directions.length][];
        while (true) {
            boolean empty = true;
            for (int layer = 0; layer < 16; layer++) {
                char[] queue = queues[layer];
                if (queue == null) continue;
                char index;
                while ((index = queue[0]) != queue[1]) {
                    queue[0]++;

                    char triple = queue[index];
                    int x = index & 15;
                    int z = (index >> 4) & 15;
                    int y = index >> 8;
                }
                queuePool.add(queue);
                queues[layer] = null;
                continue;
            }

            if (empty) break;
        }
        // empty queues

//        while (indexStart != indexEnd) {
//            char index = queue[indexStart++];
//            byte dirs = 0xF;
//            int x = index & 15;
//            int z = (index >> 4) & 15;
//            int y = index >> 8;
//
//            int layer = y >> 4;
//            long[] visitBits = visit[layer];
//
//            int x1 = x;
//            int x2 = x;
//
//            // find start of scan-line
//            int i1 = index;
//            while (true) {
//                if (x1 < 0) {
//                    // queue in adjacent chunk
//                    break;
//                }
//                if (get(visitBits, i1--)) break;
//                x1--;
//            }
//            i1++;
//            x1++;
//
//            // find end of scan-line
//            int i2 = index;
//            while (true) {
//                if (x2 > 15) {
//                    // queue in adjacent chunk
//                    break;
//                }
//                if (get(visitBits, i2++)) break;
//                x2++;
//            }
//            i2--;
//            x2--;
//
//            boolean scanUp = false;
//            boolean scanDown = false;
//            boolean scanLeft = false;
//            boolean scanRight = false;
//
//            for (int i = i1; i <= i2; i++) {
//                if (!scanDown && y > 0 && )
//            }
//
//            for (int i=x1; i<=x2; i++) { // find scan-lines above this one
//                if (!inScanLine && y>0 && ip.getPixel(i,y-1)==color)
//                {push(i, y-1); inScanLine = true;}
//                else if (inScanLine && y>0 && ip.getPixel(i,y-1)!=color)
//                    inScanLine = false;
//            }
//
//            inScanLine = false;
//            for (int i=x1; i<=x2; i++) { // find scan-lines below this one
//                if (!inScanLine && y<height-1 && ip.getPixel(i,y+1)==color)
//                {push(i, y+1); inScanLine = true;}
//                else if (inScanLine && y<height-1 && ip.getPixel(i,y+1)!=color)
//                    inScanLine = false;
//            }
//        }
    }

    public void set(long[] bits, int i) {
        bits[i >> 6] |= (1L << (i & 0x3F));
    }

    public boolean get(long[] bits, final int i) {
        return (bits[i >> 6] & (1L << (i & 0x3F))) != 0;
    }

    public char getLocalIndex(int x, int y, int z) {
        return (char) (x + (z << 4) + (y << 8));
    }


}
