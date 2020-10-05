package com.sk89q.worldedit.bukkit.adapter;

import com.boydti.fawe.beta.implementation.queue.SingleThreadQueueExtent;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class Regenerator {

    protected final org.bukkit.World originalBukkitWorld;
    protected final Region region;
    protected final Extent target;
    protected final RegenOptions options;

    //runtime
    protected SingleThreadQueueExtent source;

    public Regenerator(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
        this.originalBukkitWorld = originalBukkitWorld;
        this.region = region;
        this.target = target;
        this.options = options;
    }

    public boolean regenerate() throws Exception {
        if (!prepare())
            return false;

        try {
            if (!initNewWorld()) {
                cleanup();
                return false;
            }
        } catch (Exception e) {
            cleanup();
            throw e;
        }

        try {
            if (!generate()) {
                cleanup();
                return false;
            }
        } catch (Exception e) {
            cleanup();
            throw e;
        }

        try {
            copyToWorld();
        } catch (Exception e) {
            cleanup();
            throw e;
        }

        cleanup();
        return true;
    }

    /**
     * Implement the preparation process in here. DO NOT instanciate any variable here that require the cleanup function. This function is for gathering furhter information before initializing a new
     * world.
     *
     * @return
     */
    protected abstract boolean prepare();

    /**
     * Implement the creation of the seperate world in here.
     *
     * @return true if everything went fine, otherwise false. When false is returned the Regenerator halts the regeneration process and calls the cleanup function.
     * @throws java.lang.Exception When the implementation of this method throws and exception the Regenerator halts the regeneration process and calls the cleanup function.
     */
    protected abstract boolean initNewWorld() throws Exception;

    /**
     * Implement the chunk generation process in here. The source extent must be set in this function.
     *
     * @return true if everything went fine, otherwise false. When false is returned the Regenerator halts the regeneration process and calls the cleanup function.
     * @throws java.lang.Exception When the implementation of this method throws and exception the Regenerator halts the regeneration process and calls the cleanup function.
     */
    protected abstract boolean generate() throws Exception;

    private void copyToWorld() {
        System.out.println("Set blocks");
        long start = System.currentTimeMillis();
        boolean genbiomes = options.shouldRegenBiomes();
        for (BlockVector3 vec : region) {
            target.setBlock(vec, source.getBlock(vec));
            if (genbiomes) {
                target.setBiome(vec, source.getBiome(vec));
            }
//                    realExtent.setSkyLight(vec, extent.getSkyLight(vec));
//                    realExtent.setBlockLight(vec, extent.getBrightness(vec));
        }
        System.out.println("Finished setting blocks in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Implement the cleanup of all the mess that is created during the regeneration process (initNewWorld() and generate()).This function must not throw any exceptions.
     */
    protected abstract void cleanup();

    //algorithms
    protected List<Long> getChunkCoordsRegen(Region region, int border) { //needs to be square num of chunks
        BlockVector3 oldMin = region.getMinimumPoint();
        BlockVector3 newMin = BlockVector3.at((oldMin.getX() >> 4 << 4) - border * 16, oldMin.getY(), (oldMin.getZ() >> 4 << 4) - border * 16);
        BlockVector3 oldMax = region.getMaximumPoint();
        BlockVector3 newMax = BlockVector3.at((oldMax.getX() >> 4 << 4) + (border + 1) * 16 - 1, oldMax.getY(), (oldMax.getZ() >> 4 << 4) + (border + 1) * 16 - 1);
        Region adjustedRegion = new CuboidRegion(newMin, newMax);
        return adjustedRegion.getChunks().stream()
                .map(c -> BlockVector2.at(c.getX(), c.getZ()))
                .sorted(Comparator.<BlockVector2>comparingInt(c -> c.getZ()).thenComparingInt(c -> c.getX())) //needed for RegionLimitedWorldAccess
                .map(c -> MathMan.pairInt(c.getX(), c.getZ()))
                .collect(Collectors.toList());
    }

    /**
     * Creates a list of chunkcoord rows that may be executed concurrently
     *
     * @param allcoords the coords that should be sorted into rows, must be sorted by z and x
     * @param requiredNeighborChunkRadius the radius of neighbor chunks that may not be written to conccurently (ChunkStatus.requiredNeighborRadius)
     * @return a list of chunkcoords rows that may be executed concurrently
     */
    protected SequentialTasks<ConcurrentTasks<SequentialTasks<Long>>> getChunkStatusTaskRows(List<Long> allcoords, int requiredNeighborChunkRadius) {
        int requiredneighbors = Math.max(0, requiredNeighborChunkRadius);

        int minx = allcoords.isEmpty() ? 0 : MathMan.unpairIntX(allcoords.get(0));
        int maxx = allcoords.isEmpty() ? 0 : MathMan.unpairIntX(allcoords.get(allcoords.size() - 1));
        int minz = allcoords.isEmpty() ? 0 : MathMan.unpairIntY(allcoords.get(0));
        int maxz = allcoords.isEmpty() ? 0 : MathMan.unpairIntY(allcoords.get(allcoords.size() - 1));
        SequentialTasks<ConcurrentTasks<SequentialTasks<Long>>> tasks;
        if (maxz - minz > maxx - minx) {
            int numlists = Math.min(requiredneighbors * 2 + 1, maxx - minx + 1);

            Int2ObjectOpenHashMap<SequentialTasks<Long>> byx = new Int2ObjectOpenHashMap();
            int expectedListLength = (allcoords.size() + 1) / (maxx - minx);

            //init lists
            for (int i = minx; i <= maxx; i++) {
                byx.put(i, new SequentialTasks(expectedListLength));
            }

            //sort into lists by x coord
            for (Long xz : allcoords) {
                byx.get(MathMan.unpairIntX(xz)).add(xz);
            }

            //create parallel tasks
            tasks = new SequentialTasks(numlists);
            for (int offset = 0; offset < numlists; offset++) {
                ConcurrentTasks<SequentialTasks<Long>> para = new ConcurrentTasks((maxz - minz + 1) / numlists + 1);
                for (int i = 0; minx + i * numlists + offset <= maxx; i++)
                    para.add(byx.get(minx + i * numlists + offset));
                tasks.add(para);
            }
        } else {
            int numlists = Math.min(requiredneighbors * 2 + 1, maxz - minz + 1);

            Int2ObjectOpenHashMap<SequentialTasks<Long>> byz = new Int2ObjectOpenHashMap();
            int expectedListLength = (allcoords.size() + 1) / (maxz - minz);

            //init lists
            for (int i = minz; i <= maxz; i++) {
                byz.put(i, new SequentialTasks(expectedListLength));
            }

            //sort into lists by x coord
            for (Long xz : allcoords) {
                byz.get(MathMan.unpairIntY(xz)).add(xz);
            }

            //create parallel tasks
            tasks = new SequentialTasks(numlists);
            for (int offset = 0; offset < numlists; offset++) {
                ConcurrentTasks<SequentialTasks<Long>> para = new ConcurrentTasks((maxx - minx + 1) / numlists + 1);
                for (int i = 0; minz + i * numlists + offset <= maxz; i++)
                    para.add(byz.get(minz + i * numlists + offset));
                tasks.add(para);
            }
        }

        return tasks;
    }

    protected static Random getChunkRandom(long worldseed, int x, int z) {
        Random random = new Random();
        random.setSeed(worldseed);
        long xRand = random.nextLong() / 2L * 2L + 1L;
        long zRand = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) x * xRand + (long) z * zRand ^ worldseed);
        return random;
    }

    public enum Concurrency {
        FULL,
        RADIUS,
        NONE
    }

    public static class SequentialTasks<T> extends Tasks<T> {

        public SequentialTasks(int expectedsize) {
            super(expectedsize);
        }
    }

    public static class ConcurrentTasks<T> extends Tasks<T> {

        public ConcurrentTasks(int expectedsize) {
            super(expectedsize);
        }
    }

    public static class Tasks<T> implements Iterable<T> {

        private final List<T> tasks;

        public Tasks(int expectedsize) {
            tasks = new ArrayList(expectedsize);
        }

        public void add(T task) {
            tasks.add(task);
        }

        public List<T> list() {
            return tasks;
        }

        public int size() {
            return tasks.size();
        }
        
        @Override
        public Iterator<T> iterator() {
            return tasks.iterator();
        }

        @Override
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indent) {
            String ret = new String(new char[indent]).replace('\0', ' ') + getClass().getSimpleName() +" [\n";
            ret += new String(new char[indent]).replace('\0', ' ') + tasks.stream().map(e -> {
                if (e == null)
                    return "null";
                else if (e instanceof Tasks) {
                    return ((Tasks) e).toString(indent + 2);
                } else {
                    return e.toString();
                }
            }).collect(Collectors.joining(", "));
            return ret + new String(new char[indent]).replace('\0', ' ') + "]\n";
        }
    }
}
