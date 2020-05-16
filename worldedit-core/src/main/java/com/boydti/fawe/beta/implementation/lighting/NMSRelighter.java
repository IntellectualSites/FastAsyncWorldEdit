package com.boydti.fawe.beta.implementation.lighting;

import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.chunk.ChunkHolder;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.math.MutableBlockVector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NMSRelighter implements Relighter {
    private final IQueueExtent<IQueueChunk> queue;

    private final Map<Long, RelightSkyEntry> skyToRelight;
    private final Object present = new Object();
    private final Map<Long, Integer> chunksToSend;
    private final ConcurrentLinkedQueue<RelightSkyEntry> extentdSkyToRelight = new ConcurrentLinkedQueue<>();

    private final Map<Long, long[][][] /* z y x */ > lightQueue;
    private final AtomicBoolean lightLock = new AtomicBoolean(false);
    private final ConcurrentHashMap<Long, long[][][]> concurrentLightQueue;

    private final int maxY;

    public final MutableBlockVector3 mutableBlockPos = new MutableBlockVector3(0, 0, 0);

    private static final int DISPATCH_SIZE = 64;
    private boolean removeFirst;

    public NMSRelighter(IQueueExtent<IQueueChunk> queue) {
        this.queue = queue;
        this.skyToRelight = new Long2ObjectOpenHashMap<>();
        this.lightQueue = new Long2ObjectOpenHashMap<>();
        this.chunksToSend = new Long2ObjectOpenHashMap<>();
        this.concurrentLightQueue = new ConcurrentHashMap<>();
        this.maxY = queue.getMaxY();
    }

    @Override
    public boolean isEmpty() {
        return skyToRelight.isEmpty() && lightQueue.isEmpty() && extentdSkyToRelight.isEmpty() && concurrentLightQueue.isEmpty();
    }

    @Override
    public synchronized void removeAndRelight(boolean sky) {
        removeFirst = true;
        fixLightingSafe(sky);
        removeFirst = false;
    }

    private void set(int x, int y, int z, long[][][] map) {
        long[][] m1 = map[z];
        if (m1 == null) {
            m1 = map[z] = new long[16][];
        }
        long[] m2 = m1[x];
        if (m2 == null) {
            m2 = m1[x] = new long[4];
        }
        long value = m2[y >> 6] |= 1l << y;
    }

    public void addLightUpdate(int x, int y, int z) {
        long index = MathMan.pairInt(x >> 4, z >> 4);
        if (lightLock.compareAndSet(false, true)) {
            synchronized (lightQueue) {
                try {
                    long[][][] currentMap = lightQueue.get(index);
                    if (currentMap == null) {
                        currentMap = new long[16][][];
                        this.lightQueue.put(index, currentMap);
                    }
                    set(x & 15, y, z & 15, currentMap);
                } finally {
                    lightLock.set(false);
                }
            }
        } else {
            long[][][] currentMap = concurrentLightQueue.get(index);
            if (currentMap == null) {
                currentMap = new long[16][][];
                this.concurrentLightQueue.put(index, currentMap);
            }
            set(x & 15, y, z & 15, currentMap);
        }
    }

    public synchronized void clear() {
        extentdSkyToRelight.clear();
        skyToRelight.clear();
        chunksToSend.clear();
        lightQueue.clear();
    }

    public boolean addChunk(int cx, int cz, byte[] fix, int bitmask) {
        RelightSkyEntry toPut = new RelightSkyEntry(cx, cz, fix, bitmask);
        extentdSkyToRelight.add(toPut);
        return true;
    }

    private synchronized Map<Long, RelightSkyEntry> getSkyMap() {
        RelightSkyEntry entry;
        while ((entry = extentdSkyToRelight.poll()) != null) {
            long pair = MathMan.pairInt(entry.x, entry.z);
            RelightSkyEntry existing = skyToRelight.put(pair, entry);
            if (existing != null) {
                entry.bitmask |= existing.bitmask;
                if (entry.fix != null) {
                    for (int i = 0; i < entry.fix.length; i++) {
                        entry.fix[i] &= existing.fix[i];
                    }
                }
            }
        }
        return skyToRelight;
    }

    public synchronized void removeLighting() {
        Iterator<Map.Entry<Long, RelightSkyEntry>> iter = getSkyMap().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, RelightSkyEntry> entry = iter.next();
            RelightSkyEntry chunk = entry.getValue();
            long pair = entry.getKey();
            Integer existing = chunksToSend.get(pair);
            chunksToSend.put(pair, chunk.bitmask | (existing != null ? existing : 0));
            ChunkHolder iChunk = (ChunkHolder) queue.getOrCreateChunk(chunk.x, chunk.z);
            if (!iChunk.isInit()) {
                iChunk.init(queue, chunk.x, chunk.z);
            }
            for (int i = 0; i < 16; i++) {
                iChunk.removeSectionLighting(i, true);
            }
            iter.remove();
        }
    }

    public void updateBlockLight(Map<Long, long[][][]> map) {
        int size = map.size();
        if (size == 0) {
            System.out.println("b");
            return;
        }
        Queue<MutableBlockVector3> lightPropagationQueue = new ArrayDeque<>();
        Queue<Object[]> lightRemovalQueue = new ArrayDeque<>();
        Map<MutableBlockVector3, Object> visited = new HashMap<>();
        Map<MutableBlockVector3, Object> removalVisited = new HashMap<>();

        Iterator<Map.Entry<Long, long[][][]>> iter = map.entrySet().iterator();
        while (iter.hasNext() && size-- > 0) {
            System.out.println("c");
            Map.Entry<Long, long[][][]> entry = iter.next();
            long index = entry.getKey();
            long[][][] blocks = entry.getValue();
            System.out.println(blocks.length);
            int chunkX = MathMan.unpairIntX(index);
            int chunkZ = MathMan.unpairIntY(index);
            int bx = chunkX << 4;
            int bz = chunkZ << 4;
            ChunkHolder iChunk = (ChunkHolder) queue.getOrCreateChunk(chunkX, chunkZ);
            if (!iChunk.isInit()) {
                iChunk.init(queue, chunkX, chunkZ);
            }
            for (int lz = 0; lz < blocks.length; lz++) {
                long[][] m1 = blocks[lz];
                if (m1 == null) continue;
                for (int lx = 0; lx < m1.length; lx++) {
                    long[] m2 = m1[lx];
                    if (m2 == null) continue;
                    for (int i = 0; i < m2.length; i++) {
                        int yStart = i << 6;
                        long value = m2[i];
                        if (value != 0) {
                            for (int j = 0; j < 64; j++) {
                                if (((value >> j) & 1) == 1) {
                                    int x = lx + bx;
                                    int y = yStart + j;
                                    int z = lz + bz;
                                    int oldLevel = iChunk.getEmmittedLight(lx, y, lz);
                                    int newLevel = iChunk.getBrightness(lx, y, lz);
                                    if (oldLevel != newLevel) {
                                        iChunk.setBlockLight(x, y, z, newLevel);
                                        MutableBlockVector3 node = new MutableBlockVector3(x, y, z);
                                        if (newLevel < oldLevel) {
                                            removalVisited.put(node, present);
                                            lightRemovalQueue.add(new Object[]{node, oldLevel});
                                        } else {
                                            visited.put(node, present);
                                            lightPropagationQueue.add(node);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            iter.remove();
        }

        while (!lightRemovalQueue.isEmpty()) {
            Object[] val = lightRemovalQueue.poll();
            MutableBlockVector3 node = (MutableBlockVector3) val[0];
            int lightLevel = (int) val[1];

            this.computeRemoveBlockLight(node.getX() - 1, node.getY(), node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(node.getX() + 1, node.getY(), node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            if (node.getY() > 0) {
                this.computeRemoveBlockLight(node.getX(), node.getY() - 1, node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            }
            if (node.getY() < 255) {
                this.computeRemoveBlockLight(node.getX(), node.getY() + 1, node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            }
            this.computeRemoveBlockLight(node.getX(), node.getY(), node.getZ() - 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(node.getX(), node.getY(), node.getZ() + 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
        }

        while (!lightPropagationQueue.isEmpty()) {
            MutableBlockVector3 node = lightPropagationQueue.poll();
            int lightLevel = queue.getEmmittedLight(node.getX(), node.getY(), node.getZ());
            if (lightLevel > 1) {
                this.computeSpreadBlockLight(node.getX() - 1, node.getY(), node.getZ(), lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(node.getX() + 1, node.getY(), node.getZ(), lightLevel, lightPropagationQueue, visited);
                if (node.getY() > 0) {
                    this.computeSpreadBlockLight(node.getX(), node.getY() - 1, node.getZ(), lightLevel, lightPropagationQueue, visited);
                }
                if (node.getY() < 255) {
                    this.computeSpreadBlockLight(node.getX(), node.getY() + 1, node.getZ(), lightLevel, lightPropagationQueue, visited);
                }
                this.computeSpreadBlockLight(node.getX(), node.getY(), node.getZ() - 1, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(node.getX(), node.getY(), node.getZ() + 1, lightLevel, lightPropagationQueue, visited);
            }
        }
    }

    private void computeRemoveBlockLight(int x, int y, int z, int currentLight, Queue<Object[]> queue, Queue<MutableBlockVector3> spreadQueue, Map<MutableBlockVector3, Object> visited,
        Map<MutableBlockVector3, Object> spreadVisited) {
        int current = this.queue.getEmmittedLight(x, y, z);
        if (current != 0 && current < currentLight) {
            this.queue.setBlockLight(x, y, z, 0);
            if (current > 1) {
                if (!visited.containsKey(mutableBlockPos)) {
                    MutableBlockVector3 index = new MutableBlockVector3(x, y, z);
                    visited.put(index, present);
                    queue.add(new Object[]{index, current});
                }
            }
        } else if (current >= currentLight) {
            mutableBlockPos.setComponents(x, y, z);
            if (!spreadVisited.containsKey(mutableBlockPos)) {
                MutableBlockVector3 index = new MutableBlockVector3(x, y, z);
                spreadVisited.put(index, present);
                spreadQueue.add(index);
            }
        }
    }

    private void computeSpreadBlockLight(int x, int y, int z, int currentLight, Queue<MutableBlockVector3> queue, Map<MutableBlockVector3, Object> visited) {
        currentLight = currentLight - Math.max(1, this.queue.getOpacity(x, y, z));
        if (currentLight > 0) {
            int current = this.queue.getEmmittedLight(x, y, z);
            if (current < currentLight) {
                this.queue.setBlockLight(x, y, z, currentLight);
                mutableBlockPos.setComponents(x, y, z);
                if (!visited.containsKey(mutableBlockPos)) {
                    visited.put(new MutableBlockVector3(x, y, z), present);
                    if (currentLight > 1) {
                        queue.add(new MutableBlockVector3(x, y, z));
                    }
                }
            }
        }
    }

    public void fixLightingSafe(boolean sky) {
        if (isEmpty()) return;
        try {
            if (sky) {
                fixSkyLighting();
            } else {
                synchronized (this) {
                    Map<Long, RelightSkyEntry> map = getSkyMap();
                    Iterator<Map.Entry<Long, RelightSkyEntry>> iter = map.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<Long, RelightSkyEntry> entry = iter.next();
                        chunksToSend.put(entry.getKey(), entry.getValue().bitmask);
                        iter.remove();
                    }
                }
            }
            fixBlockLighting();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void fixBlockLighting() {
        synchronized (lightQueue) {
            while (!lightLock.compareAndSet(false, true));
            try {
                updateBlockLight(this.lightQueue);
            } finally {
                lightLock.set(false);
            }
        }
    }

    public synchronized void sendChunks() {
        Iterator<Map.Entry<Long, Integer>> iter = chunksToSend.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Integer> entry = iter.next();
            long pair = entry.getKey();
            int bitMask = entry.getValue();
            int x = MathMan.unpairIntX(pair);
            int z = MathMan.unpairIntY(pair);
            ChunkHolder chunk = (ChunkHolder) queue.getOrCreateChunk(x, z);
            chunk.setBitMask(bitMask);
            iter.remove();
        }
        if (Settings.IMP.LIGHTING.ASYNC) {
            queue.flush();
        } else {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override public void run(Object value) {
                    queue.flush();
                }
            });
        }
    }

    private boolean isTransparent(int x, int y, int z) {
        return queue.getOpacity(x, y, z) < 15;
    }

    public synchronized void fixSkyLighting() {
        // Order chunks
        Map<Long, RelightSkyEntry> map = getSkyMap();
        ArrayList<RelightSkyEntry> chunksList = new ArrayList<>(map.size());
        Iterator<Map.Entry<Long, RelightSkyEntry>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, RelightSkyEntry> entry = iter.next();
            chunksToSend.put(entry.getKey(), entry.getValue().bitmask);
            chunksList.add(entry.getValue());
            iter.remove();
        }
        Collections.sort(chunksList);
        int size = chunksList.size();
        if (size > DISPATCH_SIZE) {
            int amount = (size + DISPATCH_SIZE - 1) / DISPATCH_SIZE;
            for (int i = 0; i < amount; i++) {
                int start = i * DISPATCH_SIZE;
                int end = Math.min(size, start + DISPATCH_SIZE);
                List<RelightSkyEntry> sub = chunksList.subList(start, end);
                fixSkyLighting(sub);
            }
        } else {
            fixSkyLighting(chunksList);
        }
    }

    public void fill(byte[] mask, int chunkX, int y, int chunkZ, byte reason) {
        if (y >= 16) {
            Arrays.fill(mask, (byte) 15);
            return;
        }
        switch (reason) {
            case SkipReason.SOLID: {
                Arrays.fill(mask, (byte) 0);
                return;
            }
            case SkipReason.AIR: {
                int bx = chunkX << 4;
                int bz = chunkZ << 4;
                int index = 0;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        mask[index++] = (byte) queue.getSkyLight(bx + x, y, bz + z);
                    }
                }
            }
        }
    }

    private void fixSkyLighting(List<RelightSkyEntry> sorted) {
        RelightSkyEntry[] chunks = sorted.toArray(new RelightSkyEntry[sorted.size()]);
        boolean remove = this.removeFirst;
        BlockVectorSet chunkSet = null;
        if (remove) {
            chunkSet = new BlockVectorSet();
            BlockVectorSet tmpSet = new BlockVectorSet();
            for (RelightSkyEntry chunk : chunks) {
                tmpSet.add(chunk.x, 0, chunk.z);
            }
            for (RelightSkyEntry chunk : chunks) {
                int x = chunk.x;
                int z = chunk.z;
                if (tmpSet.contains(x + 1, 0, z) && tmpSet.contains(x - 1, 0, z) && tmpSet.contains(x, 0, z + 1) && tmpSet.contains(x, 0, z - 1)) {
                    chunkSet.add(x, 0, z);
                }
            }
        }
        for (int y = 255; y > 0; y--) {
            boolean l = true;
            for (RelightSkyEntry chunk : chunks) { // Propogate skylight
                int layer = y >> 4;
                byte[] mask = chunk.mask;
                if (chunk.fix[layer] != SkipReason.NONE) {
                    if ((y & 15) == 0 && layer != 0 && chunk.fix[layer - 1] == SkipReason.NONE) {
                        fill(mask, chunk.x, y, chunk.z, chunk.fix[layer]);
                    }
                    continue;
                }
                int bx = chunk.x << 4;
                int bz = chunk.z << 4;
                ChunkHolder iChunk = (ChunkHolder) queue.getOrCreateChunk(chunk.x, chunk.z);
                if (!iChunk.isInit()) {
                    iChunk.init(queue, chunk.x, chunk.z);
                }
                chunk.smooth = false;

                if (remove && (y & 15) == 15 && chunkSet.contains(chunk.x, 0, chunk.z)) {
                    iChunk.removeSectionLighting(y >> 4, true);
                }

                for (int j = 0; j < 256; j++) {
                    int x = j & 15;
                    int z = j >> 4;
                    byte value = mask[j];
                    byte pair = MathMan.pair16(iChunk.getOpacity(x, y, z), iChunk.getBrightness(x, y, z));
                    int opacity = MathMan.unpair16x(pair);
                    int brightness = MathMan.unpair16y(pair);
                    if (brightness > 1 && (brightness != 15 || opacity != 15)) {
                        addLightUpdate(bx + x, y, bz + z);
                    }
                    switch (value) {
                        case 0:
                            if (opacity > 1) {
                                iChunk.setSkyLight(x, y, z, 0);
                                continue;
                            }
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                            if (opacity >= value) {
                                mask[j] = 0;
                                iChunk.setSkyLight(x, y, z, 0);
                                continue;
                            }
                            if (opacity <= 1) {
                                mask[j] = --value;
                            } else {
                                mask[j] = value = (byte) Math.max(0, value - opacity);
                            }
                            break;
                        case 15:
                            if (opacity > 1) {
                                value -= opacity;
                                mask[j] = value;
                            }
                            iChunk.setSkyLight(x, y, z, value);
                            continue;
                    }
                    chunk.smooth = true;
                    iChunk.setSkyLight(x, y, z, value);
                }
            }
            for (RelightSkyEntry chunk : chunks) { // Smooth forwards
                if (chunk.smooth) {
                    smoothSkyLight(chunk, y, true);
                }
            }
            for (int i = chunks.length - 1; i >= 0; i--) { // Smooth backwards
                RelightSkyEntry chunk = chunks[i];
                if (chunk.smooth) {
                    smoothSkyLight(chunk, y, false);
                }
            }
        }
    }

    public void smoothSkyLight(RelightSkyEntry chunk, int y, boolean direction) {
        byte[] mask = chunk.mask;
        int bx = chunk.x << 4;
        int bz = chunk.z << 4;
        ChunkHolder iChunk = (ChunkHolder) queue.getOrCreateChunk(chunk.x, chunk.z);
        if (!iChunk.isInit()) {
            iChunk.init(queue, chunk.x, chunk.z);
        }
        if (direction) {
            for (int j = 0; j < 256; j++) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && iChunk.getOpacity(x, y, z) > 1)) {
                    continue;
                }
                byte value = mask[j];
                if ((value = (byte) Math.max(iChunk.getSkyLight(bx + x - 1, y, bz + z) - 1, value)) >= 14) ;
                else if ((value = (byte) Math.max(iChunk.getSkyLight(bx + x, y, bz + z - 1) - 1, value)) >= 14) ;
                if (value > mask[j]) iChunk.setSkyLight(x, y, z, mask[j] = value);
            }
        } else {
            for (int j = 255; j >= 0; j--) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && iChunk.getOpacity(x, y, z) > 1)) {
                    continue;
                }
                byte value = mask[j];
                if ((value = (byte) Math.max(iChunk.getSkyLight(bx + x + 1, y, bz + z) - 1, value)) >= 14) ;
                else if ((value = (byte) Math.max(iChunk.getSkyLight(bx + x, y, bz + z + 1) - 1, value)) >= 14) ;
                if (value > mask[j]) iChunk.setSkyLight(x, y, z, mask[j] = value);
            }
        }
    }

    public boolean isUnlit(byte[] array) {
        for (byte val : array) {
            if (val != 0) {
                return false;
            }
        }
        return true;
    }

    private class RelightSkyEntry implements Comparable {
        public final int x;
        public final int z;
        public final byte[] mask;
        public final byte[] fix;
        public int bitmask;
        public boolean smooth;

        public RelightSkyEntry(int x, int z, byte[] fix, int bitmask) {
            this.x = x;
            this.z = z;
            byte[] array = new byte[256];
            Arrays.fill(array, (byte) 15);
            this.mask = array;
            this.bitmask = bitmask;
            if (fix == null) {
                this.fix = new byte[(maxY + 1) >> 4];
                Arrays.fill(this.fix, SkipReason.NONE);
            } else {
                this.fix = fix;
            }
        }

        @Override
        public String toString() {
            return x + "," + z;
        }

        @Override
        public int compareTo(Object o) {
            RelightSkyEntry other = (RelightSkyEntry) o;
            if (other.x < x) {
                return 1;
            }
            if (other.x > x) {
                return -1;
            }
            if (other.z < z) {
                return 1;
            }
            if (other.z > z) {
                return -1;
            }
            return 0;
        }
    }
}
