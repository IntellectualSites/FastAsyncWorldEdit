package com.fastasyncworldedit.core.extent.processor.lighting;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.chunk.ChunkHolder;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class NMSRelighter implements Relighter {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final int DISPATCH_SIZE = 64;
    private static final DirectionalProperty stairDirection;
    private static final EnumProperty stairHalf;
    private static final EnumProperty stairShape;
    private static final EnumProperty slabHalf;

    static {
        stairDirection = (DirectionalProperty) (Property<?>) BlockTypes.SANDSTONE_STAIRS.getProperty("facing");
        stairHalf = (EnumProperty) (Property<?>) BlockTypes.SANDSTONE_STAIRS.getProperty("half");
        stairShape = (EnumProperty) (Property<?>) BlockTypes.SANDSTONE_STAIRS.getProperty("shape");
        slabHalf = (EnumProperty) (Property<?>) BlockTypes.SANDSTONE_SLAB.getProperty("type");
    }

    private final IQueueExtent<?> queue;
    private final Map<Long, RelightSkyEntry> skyToRelight;
    private final Object present = new Object();
    private final Map<Long, Integer> chunksToSend;
    private final ConcurrentLinkedQueue<RelightSkyEntry> extendSkyToRelight = new ConcurrentLinkedQueue<>();
    private final Map<Long, long[][][] /* z y x */> lightQueue;
    private final AtomicBoolean lightLock = new AtomicBoolean(false);
    private final ConcurrentHashMap<Long, long[][][]> concurrentLightQueue;
    private final RelightMode relightMode;
    private final int maxY;
    private final int minY;
    private final int yLongPerCol;
    private final ReentrantLock lightingLock;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private boolean removeFirst;

    public NMSRelighter(IQueueExtent<?> queue) {
        this(queue, null);
    }

    public NMSRelighter(IQueueExtent<?> queue, RelightMode relightMode) {
        this.queue = queue;
        this.skyToRelight = new Long2ObjectOpenHashMap<>(12);
        this.lightQueue = new Long2ObjectOpenHashMap<>(12);
        this.chunksToSend = new Long2ObjectOpenHashMap<>(12);
        this.concurrentLightQueue = new ConcurrentHashMap<>(12);
        this.maxY = queue.getMaxY();
        this.minY = queue.getMinY();
        this.yLongPerCol = (maxY - minY + 1) >> 6;
        this.relightMode = relightMode != null ? relightMode : RelightMode.valueOf(Settings.settings().LIGHTING.MODE);
        this.lightingLock = new ReentrantLock();
    }

    @Override
    public boolean isEmpty() {
        return skyToRelight.isEmpty() && lightQueue.isEmpty() && extendSkyToRelight.isEmpty() && concurrentLightQueue.isEmpty();
    }

    @Override
    public ReentrantLock getLock() {
        return lightingLock;
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public synchronized void removeAndRelight(boolean sky) {
        removeFirst = true;
        fixLightingSafe(sky);
        removeFirst = false;
    }

    /**
     * Utility method to reduce duplicated code to ensure values are written to long[][][] without NPEs
     *
     * @param x   x coordinate
     * @param y   y coordinate
     * @param z   z coordinate
     * @param map long[][][] to add values to
     */
    private void set(int x, int y, int z, long[][][] map) {
        long[][] m1 = map[z];
        if (m1 == null) {
            m1 = map[z] = new long[16][];
        }
        long[] m2 = m1[x];
        if (m2 == null) {
            m2 = m1[x] = new long[yLongPerCol];
        }
        // Account for negative y values by "adding" minY
        y -= minY;
        m2[y >> 6] |= 1L << (y & 63);
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
                    this.lightQueue.putAll(concurrentLightQueue);
                    concurrentLightQueue.clear();
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
        extendSkyToRelight.clear();
        skyToRelight.clear();
        chunksToSend.clear();
        lightQueue.clear();
    }

    public boolean addChunk(int cx, int cz, byte[] fix, int bitmask) {
        RelightSkyEntry toPut = new RelightSkyEntry(cx, cz, fix, bitmask, minY, maxY);
        extendSkyToRelight.add(toPut);
        return true;
    }

    private synchronized Map<Long, RelightSkyEntry> getSkyMap() {
        RelightSkyEntry entry;
        while ((entry = extendSkyToRelight.poll()) != null) {
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
            chunksToSend.compute(pair, (k, existing) -> chunk.bitmask | (existing != null ? existing : 0));
            ChunkHolder<?> iChunk = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x, chunk.z);
            if (!iChunk.isInit()) {
                iChunk.init(queue, chunk.x, chunk.z);
            }
            for (int i = minY >> 4; i <= maxY >> 4; i++) {
                iChunk.removeSectionLighting(i, true);
            }
            iter.remove();
        }
    }

    public void updateBlockLight(Map<Long, long[][][]> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        Queue<BlockVector3> lightPropagationQueue = new LinkedList<>();
        Queue<LightRemovalNode> lightRemovalQueue = new LinkedList<>();
        Map<BlockVector3, Object> visited = new HashMap<>(32);
        Map<BlockVector3, Object> removalVisited = new HashMap<>(32);

        // Make sure BlockTypes is initialised so we can check block characteristics later if needed
        BlockTypes.STONE.getMaterial();

        Iterator<Map.Entry<Long, long[][][]>> iter = map.entrySet().iterator();
        while (iter.hasNext() && size-- > 0) {
            Map.Entry<Long, long[][][]> entry = iter.next();
            long index = entry.getKey();
            long[][][] blocks = entry.getValue();
            int chunkX = MathMan.unpairIntX(index);
            int chunkZ = MathMan.unpairIntY(index);
            int bx = chunkX << 4;
            int bz = chunkZ << 4;
            ChunkHolder<?> iChunk = (ChunkHolder<?>) queue.getOrCreateChunk(chunkX, chunkZ);
            if (!iChunk.isInit()) {
                iChunk.init(queue, chunkX, chunkZ);
            }
            for (int lz = 0; lz < blocks.length; lz++) {
                long[][] m1 = blocks[lz];
                if (m1 == null) {
                    continue;
                }
                for (int lx = 0; lx < m1.length; lx++) {
                    long[] m2 = m1[lx];
                    if (m2 == null) {
                        continue;
                    }
                    for (int i = 0; i < m2.length; i++) {
                        int yStart = (i << 6) + minY;
                        long value = m2[i];
                        if (value != 0) {
                            for (int j = 0; j < 64; j++) {
                                if (((value >> j) & 1) == 1) {
                                    int x = lx + bx;
                                    int y = yStart + j;
                                    int z = lz + bz;
                                    int oldLevel = iChunk.getEmittedLight(lx, y, lz);
                                    int newLevel = iChunk.getBrightness(lx, y, lz);
                                    if (oldLevel != newLevel) {
                                        iChunk.setBlockLight(lx, y, lz, newLevel);
                                        BlockVector3 node = BlockVector3.at(x, y, z);
                                        if (newLevel < oldLevel) {
                                            removalVisited.put(node, present);
                                            lightRemovalQueue.add(new LightRemovalNode(node, oldLevel));
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
            LightRemovalNode val = lightRemovalQueue.poll();
            BlockVector3 node = val.pos();
            int lightLevel = val.previous();

            this.computeRemoveBlockLight(
                    node.x() - 1,
                    node.y(),
                    node.z(),
                    lightLevel,
                    lightRemovalQueue,
                    lightPropagationQueue,
                    removalVisited,
                    visited
            );
            this.computeRemoveBlockLight(
                    node.x() + 1,
                    node.y(),
                    node.z(),
                    lightLevel,
                    lightRemovalQueue,
                    lightPropagationQueue,
                    removalVisited,
                    visited
            );
            if (node.y() > minY) {
                this.computeRemoveBlockLight(
                        node.x(),
                        node.y() - 1,
                        node.z(),
                        lightLevel,
                        lightRemovalQueue,
                        lightPropagationQueue,
                        removalVisited,
                        visited
                );
            }
            if (node.y() < maxY) {
                this.computeRemoveBlockLight(
                        node.x(),
                        node.y() + 1,
                        node.z(),
                        lightLevel,
                        lightRemovalQueue,
                        lightPropagationQueue,
                        removalVisited,
                        visited
                );
            }
            this.computeRemoveBlockLight(
                    node.x(),
                    node.y(),
                    node.z() - 1,
                    lightLevel,
                    lightRemovalQueue,
                    lightPropagationQueue,
                    removalVisited,
                    visited
            );
            this.computeRemoveBlockLight(
                    node.x(),
                    node.y(),
                    node.z() + 1,
                    lightLevel,
                    lightRemovalQueue,
                    lightPropagationQueue,
                    removalVisited,
                    visited
            );
        }

        while (!lightPropagationQueue.isEmpty()) {
            BlockVector3 node = lightPropagationQueue.poll();
            ChunkHolder<?> iChunk = (ChunkHolder<?>) queue.getOrCreateChunk(node.x() >> 4, node.z() >> 4);
            if (!iChunk.isInit()) {
                iChunk.init(queue, node.x() >> 4, node.z() >> 4);
            }
            int lightLevel = iChunk.getEmittedLight(node.x() & 15, node.y(), node.z() & 15);
            BlockState state = this.queue.getBlock(node.x(), node.y(), node.z());
            String id = state.getBlockType().id().toLowerCase(Locale.ROOT);
            if (lightLevel <= 1) {
                continue;
            }
            if (id.contains("slab")) {
                boolean top = state.getState(slabHalf).equalsIgnoreCase("top");
                computeSlab(node.x(), node.y(), node.z(), lightLevel, lightPropagationQueue, visited, top);
            } else if (id.contains("stair")) {
                boolean top = state.getState(stairHalf).equalsIgnoreCase("top");
                Direction direction = getStairDir(state);
                String shape = getStairShape(state);
                computeStair(node.x(), node.y(), node.z(), lightLevel, lightPropagationQueue, visited, top, direction, shape);
            } else {
                computeNormal(node.x(), node.y(), node.z(), lightLevel, lightPropagationQueue, visited);
            }
        }
    }

    private void computeStair(
            int x,
            int y,
            int z,
            int currentLight,
            Queue<BlockVector3> queue,
            Map<BlockVector3, Object> visited,
            boolean top,
            Direction direction,
            String shape
    ) {
        east:
        {
            // Block East
            if (direction != Direction.WEST && (direction == Direction.NORTH && shape.equals("inner_right") || direction == Direction.SOUTH && shape.equals(
                    "inner_left") || direction == Direction.EAST && !shape.startsWith("outer"))) {
                break east;
            }
            BlockState state = this.queue.getBlock(x + 1, y, z);
            if (!(checkStairEast(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom"))) {
                break east;
            }
            if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
                this.computeSpreadBlockLight(x + 1, y, z, currentLight, queue, visited);
                break east;
            }
            Direction otherDir = getStairDir(state);
            String otherShape = getStairShape(state);
            boolean b1 = (otherDir == Direction.NORTH && !otherShape.equals("outer_right")) || (otherDir == Direction.EAST && otherShape.equals(
                    "inner_left"));
            boolean b2 = (otherDir == Direction.SOUTH && !otherShape.equals("outer_left")) || (otherDir == Direction.EAST && otherShape.equals(
                    "inner_right"));
            switch (direction) {
                case EAST:
                    if (shape.equals("outer_right") && b1) {
                        break east;
                    } else if (shape.equals("outer_left") && b2) {
                        break east;
                    }
                    break;
                case WEST:
                    if (shape.equals("straight") || shape.startsWith("outer")) {
                        break;
                    } else if (shape.equals("inner_left") && b1) {
                        break east;
                    } else if (shape.equals("inner_right") && b2) {
                        break east;
                    }
                    break;
                case SOUTH:
                    if (b1 || (otherDir == Direction.SOUTH && otherShape.equals("inner_right"))) {
                        break east;
                    }
                    break;
                case NORTH:
                    if (b2 || (otherDir == Direction.NORTH && otherShape.equals("inner_left"))) {
                        break east;
                    }
                    break;
            }
            this.computeSpreadBlockLight(x + 1, y, z, currentLight, queue, visited);
        }
        west:
        {
            // Block West
            if (direction != Direction.EAST && (direction == Direction.SOUTH && shape.equals("inner_right") || direction == Direction.NORTH && shape.equals(
                    "inner_left") || direction == Direction.WEST && !shape.startsWith("outer"))) {
                break west;
            }
            BlockState state = this.queue.getBlock(x - 1, y, z);
            if (!(checkStairWest(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom"))) {
                break west;
            }
            if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
                this.computeSpreadBlockLight(x - 1, y, z, currentLight, queue, visited);
                break west;
            }
            Direction otherDir = getStairDir(state);
            String otherShape = getStairShape(state);
            boolean b1 = (otherDir == Direction.SOUTH && !otherShape.equals("outer_right")) || (otherDir == Direction.WEST && otherShape.equals(
                    "inner_left"));
            boolean b2 = (otherDir == Direction.NORTH && !otherShape.equals("outer_left")) || (otherDir == Direction.WEST && otherShape.equals(
                    "inner_right"));
            switch (direction) {
                case WEST:
                    if (shape.equals("outer_right") && b1) {
                        break west;
                    } else if (shape.equals("outer_left") && b2) {
                        break west;
                    }
                    break;
                case EAST:
                    if (shape.equals("straight") || shape.startsWith("outer")) {
                        break;
                    } else if (shape.equals("inner_left") && b1) {
                        break west;
                    } else if (shape.equals("inner_right") && b2) {
                        break west;
                    }
                    break;
                case NORTH:
                    if (b1 || (otherDir == Direction.NORTH && otherShape.equals("inner_right"))) {
                        break west;
                    }
                    break;
                case SOUTH:
                    if (b2 || (otherDir == Direction.SOUTH && otherShape.equals("inner_left"))) {
                        break west;
                    }
                    break;
            }
            this.computeSpreadBlockLight(x - 1, y, z, currentLight, queue, visited);
        }
        south:
        {
            // Block South
            if (direction != Direction.NORTH && (direction == Direction.WEST && shape.equals("inner_right") || direction == Direction.EAST && shape.equals(
                    "inner_left") || direction == Direction.SOUTH && !shape.startsWith("outer"))) {
                break south;
            }
            BlockState state = this.queue.getBlock(x, y, z + 1);
            if (!(checkStairSouth(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom"))) {
                break south;
            }
            if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
                this.computeSpreadBlockLight(x, y, z + 1, currentLight, queue, visited);
                break south;
            }
            Direction otherDir = getStairDir(state);
            String otherShape = getStairShape(state);
            boolean b1 = (otherDir == Direction.EAST && !otherShape.equals("outer_right")) || (otherDir == Direction.SOUTH && otherShape.equals(
                    "inner_left"));
            boolean b2 = (otherDir == Direction.WEST && !otherShape.equals("outer_left")) || (otherDir == Direction.SOUTH && otherShape.equals(
                    "inner_right"));
            switch (direction) {
                case SOUTH:
                    if (shape.equals("outer_right") && b1) {
                        break south;
                    } else if (shape.equals("outer_left") && b2) {
                        break south;
                    }
                    break;
                case NORTH:
                    if (shape.equals("straight") || shape.startsWith("outer")) {
                        break;
                    } else if (shape.equals("inner_left") && b1) {
                        break south;
                    } else if (shape.equals("inner_right") && b2) {
                        break south;
                    }
                    break;
                case WEST:
                    if (b1 || (otherDir == Direction.WEST && otherShape.equals("inner_right"))) {
                        break south;
                    }
                    break;
                case EAST:
                    if (b2 || (otherDir == Direction.EAST && otherShape.equals("inner_left"))) {
                        break south;
                    }
                    break;
            }
            this.computeSpreadBlockLight(x, y, z + 1, currentLight, queue, visited);
        }
        north:
        {
            // Block North
            if (direction != Direction.SOUTH && (direction == Direction.EAST && shape.equals("inner_right") || direction == Direction.WEST && shape.equals(
                    "inner_left") || direction == Direction.NORTH && !shape.startsWith("outer"))) {
                break north;
            }
            BlockState state = this.queue.getBlock(x, y, z - 1);
            if (!(checkStairNorth(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom"))) {
                break north;
            }
            if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
                this.computeSpreadBlockLight(x, y, z - 1, currentLight, queue, visited);
                break north;
            }
            Direction otherDir = getStairDir(state);
            String otherShape = getStairShape(state);
            boolean b1 = (otherDir == Direction.WEST && !otherShape.equals("outer_right")) || (otherDir == Direction.NORTH && otherShape.equals(
                    "inner_left"));
            boolean b2 = (otherDir == Direction.EAST && !otherShape.equals("outer_left")) || (otherDir == Direction.NORTH && otherShape.equals(
                    "inner_right"));
            switch (direction) {
                case NORTH:
                    if (shape.equals("outer_right") && b1) {
                        break north;
                    } else if (shape.equals("outer_left") && b2) {
                        break north;
                    }
                    break;
                case SOUTH:
                    if (shape.equals("straight") || shape.startsWith("outer")) {
                        break;
                    } else if (shape.equals("inner_left") && b1) {
                        break north;
                    } else if (shape.equals("inner_right") && b2) {
                        break north;
                    }
                    break;
                case EAST:
                    if (b1 || (otherDir == Direction.EAST && otherShape.equals("inner_right"))) {
                        break north;
                    }
                    break;
                case WEST:
                    if (b2 || (otherDir == Direction.WEST && otherShape.equals("inner_left"))) {
                        break north;
                    }
                    break;
            }
            this.computeSpreadBlockLight(x, y, z - 1, currentLight, queue, visited);
        }
        computeUpDown(x, y, z, currentLight, queue, visited, top);

    }

    private void computeSlab(
            int x,
            int y,
            int z,
            int currentLight,
            Queue<BlockVector3> queue,
            Map<BlockVector3, Object> visited,
            boolean top
    ) {
        {
            // Block East
            BlockState state = this.queue.getBlock(x + 1, y, z);
            if (checkStairEast(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom")) {
                this.computeSpreadBlockLight(x + 1, y, z, currentLight, queue, visited);
            }
        }
        {
            // Block West
            BlockState state = this.queue.getBlock(x - 1, y, z);
            if (checkStairWest(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom")) {
                this.computeSpreadBlockLight(x - 1, y, z, currentLight, queue, visited);
            }
        }
        {
            // Block South
            BlockState state = this.queue.getBlock(x, y, z + 1);
            if (checkStairSouth(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom")) {
                this.computeSpreadBlockLight(x, y, z + 1, currentLight, queue, visited);
            }
        }
        {
            // Block North
            BlockState state = this.queue.getBlock(x, y, z - 1);
            if (checkStairNorth(state) && isStairOrTrueTop(state, top) && isSlabOrTrueValue(state, top ? "top" : "bottom")) {
                this.computeSpreadBlockLight(x, y, z - 1, currentLight, queue, visited);
            }
        }
        computeUpDown(x, y, z, currentLight, queue, visited, top);
    }

    private void computeUpDown(
            int x,
            int y,
            int z,
            int currentLight,
            Queue<BlockVector3> queue,
            Map<BlockVector3, Object> visited,
            boolean top
    ) {
        BlockState state = this.queue.getBlock(x, y - 1, z);
        if (y > 0 && top && isSlabOrTrueValue(state, "bottom") && isStairOrTrueTop(state, false)) {
            this.computeSpreadBlockLight(x, y - 1, z, currentLight, queue, visited);
        }
        state = this.queue.getBlock(x, y + 1, z);
        if (y < maxY && !top && isSlabOrTrueValue(state, "top") && isStairOrTrueTop(state, true)) {
            this.computeSpreadBlockLight(x, y + 1, z, currentLight, queue, visited);
        }
    }

    private void computeNormal(
            int x,
            int y,
            int z,
            int currentLight,
            Queue<BlockVector3> queue,
            Map<BlockVector3, Object> visited
    ) {
        {
            // Block East
            BlockState state = this.queue.getBlock(x + 1, y, z);
            if (checkStairEast(state) && (isSlabOrTrueValue(state, "top") || isSlabOrTrueValue(state, "bottom"))) {
                this.computeSpreadBlockLight(x + 1, y, z, currentLight, queue, visited);
            }
        }
        {
            // Block West
            BlockState state = this.queue.getBlock(x - 1, y, z);
            if (checkStairWest(state) && (isSlabOrTrueValue(state, "top") || isSlabOrTrueValue(state, "bottom"))) {
                this.computeSpreadBlockLight(x - 1, y, z, currentLight, queue, visited);
            }
        }
        {
            // Block South
            BlockState state = this.queue.getBlock(x, y, z + 1);
            if (checkStairSouth(state) && (isSlabOrTrueValue(state, "top") || isSlabOrTrueValue(state, "bottom"))) {
                this.computeSpreadBlockLight(x, y, z + 1, currentLight, queue, visited);
            }
        }
        {
            // Block North
            BlockState state = this.queue.getBlock(x, y, z - 1);
            if (checkStairNorth(state) && (isSlabOrTrueValue(state, "top") || isSlabOrTrueValue(state, "bottom"))) {
                this.computeSpreadBlockLight(x, y, z - 1, currentLight, queue, visited);
            }
        }
        BlockState state = this.queue.getBlock(x, y - 1, z);
        if (y > 0 && isSlabOrTrueValue(state, "bottom") && isStairOrTrueTop(state, false)) {
            this.computeSpreadBlockLight(x, y - 1, z, currentLight, queue, visited);
        }
        state = this.queue.getBlock(x, y + 1, z);
        if (y < maxY && isSlabOrTrueValue(state, "top") && isStairOrTrueTop(state, false)) {
            this.computeSpreadBlockLight(x, y + 1, z, currentLight, queue, visited);
        }
    }

    private boolean checkStairNorth(BlockState state) {
        if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
            return true;
        }
        Direction direction = getStairDir(state);
        String shape = getStairShape(state);
        if (shape.startsWith("outer") || direction == Direction.NORTH) {
            return true;
        }
        if (direction == Direction.SOUTH) {
            return false;
        }
        if (direction == Direction.WEST) {
            return !shape.equals("inner_left");
        }
        return direction != Direction.EAST || !shape.equals("inner_right");
    }

    private boolean checkStairSouth(BlockState state) {
        if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
            return true;
        }
        Direction direction = getStairDir(state);
        String shape = getStairShape(state);
        if (shape.startsWith("outer") || direction == Direction.SOUTH) {
            return true;
        }
        if (direction == Direction.NORTH) {
            return false;
        }
        if (direction == Direction.EAST) {
            return !shape.equals("inner_left");
        }
        return direction != Direction.WEST || !shape.equals("inner_right");
    }

    private boolean checkStairEast(BlockState state) {
        if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
            return true;
        }
        Direction direction = getStairDir(state);
        String shape = getStairShape(state);
        if (shape.startsWith("outer") || direction == Direction.EAST) {
            return true;
        }
        if (direction == Direction.WEST) {
            return false;
        }
        if (direction == Direction.NORTH) {
            return !shape.equals("inner_left");
        }
        return direction != Direction.SOUTH || !shape.equals("inner_right");
    }

    private boolean checkStairWest(BlockState state) {
        if (!state.getBlockType().id().toLowerCase(Locale.ROOT).contains("stair")) {
            return true;
        }
        Direction direction = getStairDir(state);
        String shape = getStairShape(state);
        if (shape.startsWith("outer") || direction == Direction.WEST) {
            return true;
        }
        if (direction == Direction.EAST) {
            return false;
        }
        if (direction == Direction.SOUTH) {
            return !shape.equals("inner_left");
        }
        return direction != Direction.NORTH || !shape.equals("inner_right");
    }

    private Direction getStairDir(BlockState state) {
        return state.getState(stairDirection);
    }

    private String getStairShape(BlockState state) {
        return state.getState(stairShape).toLowerCase(Locale.ROOT);
    }

    private boolean isStairOrTrueTop(BlockState state, boolean top) {
        return !state.getBlockType().id().contains("stair") || state.getState(stairHalf).equals("top") == top;
    }

    private boolean isSlabOrTrueValue(BlockState state, String value) {
        return !state.getBlockType().id().contains("slab") || state.getState(slabHalf).equals(value);
    }

    private void computeRemoveBlockLight(
            int x,
            int y,
            int z,
            int currentLight,
            Queue<LightRemovalNode> queue,
            Queue<BlockVector3> spreadQueue,
            Map<BlockVector3, Object> visited,
            Map<BlockVector3, Object> spreadVisited
    ) {
        ChunkHolder<?> iChunk = (ChunkHolder<?>) this.queue.getOrCreateChunk(x >> 4, z >> 4);
        if (!iChunk.isInit()) {
            iChunk.init(this.queue, x >> 4, z >> 4);
        }
        int current = iChunk.getEmittedLight(x & 15, y, z & 15);
        if (current != 0 && current < currentLight) {
            iChunk.setBlockLight(x, y, z, 0);
            if (current > 1) {
                visited.computeIfAbsent(
                        BlockVector3.at(x, y, z), k -> {
                            queue.add(new LightRemovalNode(k, current));
                            return present;
                        }
                );
            }
        } else if (current >= currentLight) {
            spreadVisited.computeIfAbsent(
                    BlockVector3.at(x, y, z), k -> {
                        spreadQueue.add(k);
                        return present;
                    }
            );
        }
    }

    private void computeSpreadBlockLight(
            int x,
            int y,
            int z,
            int currentLight,
            Queue<BlockVector3> queue,
            Map<BlockVector3, Object> visited
    ) {
        BlockMaterial material = this.queue.getBlock(x, y, z).getMaterial();
        boolean solidNeedsLight = (!material.isSolid() || !material.isFullCube()) && material.getLightOpacity() > 0 && material.getLightValue() == 0;
        int correctedLight = !solidNeedsLight ? currentLight - Math.max(1, material.getLightOpacity()) : currentLight - 1;
        if (currentLight > 0) {
            ChunkHolder<?> iChunk = (ChunkHolder<?>) this.queue.getOrCreateChunk(x >> 4, z >> 4);
            if (!iChunk.isInit()) {
                iChunk.init(this.queue, x >> 4, z >> 4);
            }
            int current = iChunk.getEmittedLight(x & 15, y, z & 15);
            if (currentLight > current) {
                iChunk.setBlockLight(x & 15, y, z & 15, currentLight);
                visited.computeIfAbsent(
                        BlockVector3.at(x, y, z), k -> {
                            if (correctedLight > 1) {
                                queue.add(k);
                            }
                            return present;
                        }
                );
            }
        }
    }

    public void fixLightingSafe(boolean sky) {
        if (isEmpty()) {
            return;
        }
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
        sendChunks();
    }

    public void fixBlockLighting() {
        synchronized (lightQueue) {
            while (!lightLock.compareAndSet(false, true)) {
                try {
                    lightLock.wait(50);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
            }
            try {
                updateBlockLight(this.lightQueue);
            } finally {
                lightLock.set(false);
            }
        }
    }

    @Override
    public synchronized void close() {
        Iterator<Map.Entry<Long, Integer>> iter = chunksToSend.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Integer> entry = iter.next();
            long pair = entry.getKey();
            int bitMask = entry.getValue();
            int x = MathMan.unpairIntX(pair);
            int z = MathMan.unpairIntY(pair);
            ChunkHolder<?> chunk = (ChunkHolder<?>) queue.getOrCreateChunk(x, z);
            chunk.setBitMask(bitMask);
            iter.remove();
        }
        if (Settings.settings().LIGHTING.ASYNC) {
            queue.flush();
            finished.set(true);
        } else {
            TaskManager.taskManager().sync(new RunnableVal<>() {
                @Override
                public void run(Object value) {
                    queue.flush();
                    finished.set(true);
                }
            });
        }
    }

    public void flush() {
        close();
    }

    public synchronized void sendChunks() {
        RunnableVal<Object> runnable = new RunnableVal<>() {
            @Override
            public void run(Object value) {
                Iterator<Map.Entry<Long, Integer>> iter = chunksToSend.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Long, Integer> entry = iter.next();
                    long pair = entry.getKey();
                    int bitMask = entry.getValue();
                    int x = MathMan.unpairIntX(pair);
                    int z = MathMan.unpairIntY(pair);
                    ChunkHolder<?> chunk = (ChunkHolder<?>) queue.getOrCreateChunk(x, z);
                    chunk.setBitMask(bitMask);
                    chunk.flushLightToGet();
                    Fawe.platform().getPlatformAdapter().sendChunk(chunk.getOrCreateGet(), bitMask, true);
                    iter.remove();
                }
                finished.set(true);
            }
        };
        if (Settings.settings().LIGHTING.ASYNC) {
            runnable.run();
        } else {
            TaskManager.taskManager().sync(runnable);
        }
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

    public void fill(byte[] mask, ChunkHolder<?> iChunk, int y, byte reason) {
        if (y >= 16) {
            Arrays.fill(mask, (byte) 15);
            return;
        }
        switch (reason) {
            case SkipReason.SOLID -> {
                Arrays.fill(mask, (byte) 0);
            }
            case SkipReason.AIR -> {
                int index = 0;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        mask[index++] = (byte) iChunk.getSkyLight(x, y, z);
                    }
                }
            }
        }
    }

    private void fixSkyLighting(List<RelightSkyEntry> sorted) {
        RelightSkyEntry[] chunks = sorted.toArray(new RelightSkyEntry[0]);
        boolean remove = this.removeFirst;
        BlockVectorSet chunkSet = null;
        if (remove) {
            BlockVectorSet tmpSet = new BlockVectorSet();
            chunkSet = new BlockVectorSet();
            for (RelightSkyEntry chunk : chunks) {
                tmpSet.add(chunk.x, 0, chunk.z);
            }
            for (RelightSkyEntry chunk : chunks) {
                int x = chunk.x;
                int z = chunk.z;
                if (tmpSet.contains(x + 1, 0, z) && tmpSet.contains(x - 1, 0, z) && tmpSet.contains(
                        x,
                        0,
                        z + 1
                ) && tmpSet.contains(x, 0, z - 1)) {
                    chunkSet.add(x, 0, z);
                }
            }
        }
        for (int y = maxY; y > minY; y--) {
            for (RelightSkyEntry chunk : chunks) { // Propagate skylight
                int layer = (y - minY) >> 4;
                byte[] mask = chunk.mask;
                int bx = chunk.x << 4;
                int bz = chunk.z << 4;
                ChunkHolder<?> iChunk = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x, chunk.z);
                if (chunk.fix[layer] != SkipReason.NONE) {
                    if ((y & 15) == 0 && layer != 0 && chunk.fix[layer - 1] == SkipReason.NONE) {
                        fill(mask, iChunk, y, chunk.fix[layer]);
                    }
                    continue;
                }
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
                    BlockState state = iChunk.getBlock(x, y, z);
                    BlockMaterial material = state.getMaterial();
                    int opacity = material.getLightOpacity();
                    int brightness = material.getLightValue();
                    if (brightness > 0 && brightness != iChunk.getEmittedLight(x, y, z)) {
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
                                if (!isStairOrTrueTop(state, true) || !(isSlabOrTrueValue(
                                        state,
                                        "top"
                                ) || isSlabOrTrueValue(state, "double"))) {
                                    iChunk.setSkyLight(x, y, z, value);
                                } else {
                                    iChunk.setSkyLight(x, y, z, 0);
                                }
                                continue;
                            }
                            if (opacity <= 1) {
                                mask[j] = --value;
                            } else {
                                mask[j] = value = (byte) Math.max(0, value - opacity);
                            }
                            break;
                        case 15:
                            if (opacity > 0) {
                                value -= (byte) opacity;
                                mask[j] = value;
                            }
                            if (!isStairOrTrueTop(state, true) || !(isSlabOrTrueValue(state, "top") || isSlabOrTrueValue(
                                    state,
                                    "double"
                            ))) {
                                iChunk.setSkyLight(x, y, z, value + opacity);
                            } else {
                                iChunk.setSkyLight(x, y, z, value);
                            }
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

    private void smoothSkyLight(RelightSkyEntry chunk, int y, boolean direction) {
        byte[] mask = chunk.mask;
        ChunkHolder<?> iChunk = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x, chunk.z);
        ChunkHolder<?> iChunkx;
        ChunkHolder<?> iChunkz;
        if (!iChunk.isInit()) {
            iChunk.init(queue, chunk.x, chunk.z);
        }
        if (direction) {
            iChunkx = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x - 1, chunk.z);
            iChunkz = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x, chunk.z - 1);
            if (!iChunkx.isInit()) {
                iChunkx.init(queue, chunk.x - 1, chunk.z);
            }
            if (!iChunkz.isInit()) {
                iChunkz.init(queue, chunk.x, chunk.z - 1);
            }
            for (int j = 0; j < 256; j++) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && iChunk.getOpacity(x, y, z) > 1)) {
                    continue;
                }
                byte value = mask[j];
                if (x != 0 && z != 0) {
                    if ((value = (byte) Math.max(iChunk.getSkyLight(x - 1, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunk.getSkyLight(x, y, z - 1) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                } else if (x == 0 && z == 0) {
                    if ((value = (byte) Math.max(iChunkx.getSkyLight(15, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunkz.getSkyLight(x, y, 15) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                } else if (x == 0) {
                    if ((value = (byte) Math.max(iChunkx.getSkyLight(15, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunk.getSkyLight(x, y, z - 1) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                } else {
                    if ((value = (byte) Math.max(iChunk.getSkyLight(x - 1, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunkz.getSkyLight(x, y, 15) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                }
            }
        } else {
            iChunkx = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x + 1, chunk.z);
            iChunkz = (ChunkHolder<?>) queue.getOrCreateChunk(chunk.x, chunk.z + 1);
            if (!iChunkx.isInit()) {
                iChunkx.init(queue, chunk.x - 1, chunk.z);
            }
            if (!iChunkz.isInit()) {
                iChunkz.init(queue, chunk.x, chunk.z - 1);
            }
            for (int j = 255; j >= 0; j--) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && iChunk.getOpacity(x, y, z) > 1)) {
                    continue;
                }
                byte value = mask[j];
                if (x != 15 && z != 15) {
                    if ((value = (byte) Math.max(iChunk.getSkyLight(x + 1, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunk.getSkyLight(x, y, z + 1) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                } else if (x == 15 && z == 15) {
                    if ((value = (byte) Math.max(iChunkx.getSkyLight(0, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunkz.getSkyLight(x, y, 0) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                } else if (x == 15) {
                    if ((value = (byte) Math.max(iChunkx.getSkyLight(0, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunk.getSkyLight(x, y, z + 1) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                } else {
                    if ((value = (byte) Math.max(iChunk.getSkyLight(x + 1, y, z) - 1, value)) < 14) {
                        value = (byte) Math.max(iChunkz.getSkyLight(x, y, 0) - 1, value);
                    }
                    if (value > mask[j]) {
                        iChunk.setSkyLight(x, y, z, mask[j] = value);
                    }
                }
            }
        }
    }

    private record LightRemovalNode(BlockVector3 pos, int previous) {

    }

    private static class RelightSkyEntry implements Comparable<RelightSkyEntry> {

        private static final Comparator<RelightSkyEntry> COMPARATOR = Comparator
                .comparingInt(RelightSkyEntry::x)
                .thenComparingInt(RelightSkyEntry::z);

        public final int x;
        public final int z;
        public final byte[] mask;
        public final byte[] fix;
        public int bitmask;
        public boolean smooth;

        private RelightSkyEntry(int x, int z, byte[] fix, int bitmask, int minY, int maxY) {
            this.x = x;
            this.z = z;
            byte[] array = new byte[256];
            Arrays.fill(array, (byte) 15);
            this.mask = array;
            this.bitmask = bitmask;
            if (fix == null) {
                this.fix = new byte[(maxY - minY + 1) >> 4];
                Arrays.fill(this.fix, SkipReason.NONE);
            } else {
                this.fix = fix;
            }
        }

        private int x() {
            return x;
        }

        private int z() {
            return z;
        }

        //Following are public because they are public in Object. NONE of this nested class is API.
        @Override
        public String toString() {
            return x + "," + z;
        }

        @Override
        public int compareTo(@Nonnull RelightSkyEntry o) {
            return COMPARATOR.compare(this, o);
        }

    }

}
