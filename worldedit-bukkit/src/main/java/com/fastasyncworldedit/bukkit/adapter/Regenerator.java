package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.SingleThreadQueueExtent;
import com.fastasyncworldedit.core.util.MathMan;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents an abstract regeneration handler.
 *
 * @param <IChunkAccess> the type of the {@code IChunkAccess} of the current Minecraft implementation
 * @param <ProtoChunk>   the type of the {@code ProtoChunk} of the current Minecraft implementation
 * @param <Chunk>        the type of the {@code Chunk} of the current Minecraft implementation
 * @param <ChunkStatus>  the type of the {@code ChunkStatusWrapper} wrapping the {@code ChunkStatus} enum
 */
public abstract class Regenerator<IChunkAccess, ProtoChunk extends IChunkAccess, Chunk extends IChunkAccess, ChunkStatus extends Regenerator.ChunkStatusWrapper<IChunkAccess>> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected final org.bukkit.World originalBukkitWorld;
    protected final Region region;
    protected final Extent target;
    protected final RegenOptions options;

    //runtime
    protected final Map<ChunkStatus, Concurrency> chunkStati = new LinkedHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<ProtoChunk> protoChunks = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    protected boolean generateConcurrent = true;
    protected long seed;
    private ExecutorService executor;
    private SingleThreadQueueExtent source;

    /**
     * Initializes an abstract regeneration handler.
     *
     * @param originalBukkitWorld the Bukkit world containing all the information on how to regenerate the {code Region}
     * @param region              the selection to regenerate
     * @param target              the target {@code Extent} to paste the regenerated blocks into
     * @param options             the options to used while regenerating and pasting into the target {@code Extent}
     */
    public Regenerator(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
        this.originalBukkitWorld = originalBukkitWorld;
        this.region = region;
        this.target = target;
        this.options = options;
    }

    private static Random getChunkRandom(long worldseed, int x, int z) {
        Random random = new Random();
        random.setSeed(worldseed);
        long xRand = random.nextLong() / 2L * 2L + 1L;
        long zRand = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) x * xRand + (long) z * zRand ^ worldseed);
        return random;
    }

    /**
     * Regenerates the selected {@code Region}.
     *
     * @return whether or not the regeneration process was successful
     * @throws Exception when something goes terribly wrong
     */
    public boolean regenerate() throws Exception {
        if (!prepare()) {
            return false;
        }

        try {
            if (!initNewWorld()) {
                cleanup0();
                return false;
            }
        } catch (Exception e) {
            cleanup0();
            throw e;
        }

        try {
            if (!generate()) {
                cleanup0();
                return false;
            }
        } catch (Exception e) {
            cleanup0();
            throw e;
        }

        try {
            copyToWorld();
        } catch (Exception e) {
            cleanup0();
            throw e;
        }

        cleanup0();
        return true;
    }

    /**
     * Returns the {@code ProtoChunk} at the given chunk coordinates.
     *
     * @param x the chunk x coordinate
     * @param z the chunk z coordinate
     * @return the {@code ProtoChunk} at the given chunk coordinates or null if it is not part of the regeneration process or has not been initialized yet.
     */
    protected ProtoChunk getProtoChunkAt(int x, int z) {
        return protoChunks.get(MathMan.pairInt(x, z));
    }

    /**
     * Returns the {@code Chunk} at the given chunk coordinates.
     *
     * @param x the chunk x coordinate
     * @param z the chunk z coordinate
     * @return the {@code Chunk} at the given chunk coordinates or null if it is not part of the regeneration process or has not been converted yet.
     */
    protected Chunk getChunkAt(int x, int z) {
        return chunks.get(MathMan.pairInt(x, z));
    }

    private boolean generate() throws Exception {
        if (generateConcurrent) {
            //Using concurrent chunk generation
            executor = Executors.newFixedThreadPool(Settings.settings().QUEUE.PARALLEL_THREADS, new ThreadFactoryBuilder()
                    .setNameFormat("fawe-regen-%d")
                    .build()
            );
        } else { // else using sequential chunk generation, concurrent not supported
            executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("fawe-regen-%d")
                    .build());
        }

        //TODO: can we get that required radius down without affecting chunk generation (e.g. strucures, features, ...)?
        //for now it is working well and fast, if we are bored in the future we could do the research (a lot of it) to reduce the border radius

        //generate chunk coords lists with a certain radius
        Int2ObjectOpenHashMap<List<Long>> chunkCoordsForRadius = new Int2ObjectOpenHashMap<>();
        chunkStati.keySet().stream().map(ChunkStatusWrapper::requiredNeighborChunkRadius0).distinct().forEach(radius -> {
            if (radius == -1) { //ignore ChunkStatus.EMPTY
                return;
            }
            int border = 10 - radius; //9 = 8 + 1, 8: max border radius used in chunk stages, 1: need 1 extra chunk for chunk
            // features to generate at the border of the region
            chunkCoordsForRadius.put(radius, getChunkCoordsRegen(region, border));
        });

        //create chunks
        for (Long xz : chunkCoordsForRadius.get(0)) {
            ProtoChunk chunk = createProtoChunk(MathMan.unpairIntX(xz), MathMan.unpairIntY(xz));
            protoChunks.put(xz, chunk);
        }

        //generate lists for RegionLimitedWorldAccess, need to be square with odd length (e.g. 17x17), 17 = 1 middle chunk + 8 border chunks * 2
        Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<List<IChunkAccess>>> worldlimits = new Int2ObjectOpenHashMap<>();
        chunkStati.keySet().stream().map(ChunkStatusWrapper::requiredNeighborChunkRadius0).distinct().forEach(radius -> {
            if (radius == -1) { //ignore ChunkStatus.EMPTY
                return;
            }
            Long2ObjectOpenHashMap<List<IChunkAccess>> map = new Long2ObjectOpenHashMap<>();
            for (Long xz : chunkCoordsForRadius.get(radius)) {
                int x = MathMan.unpairIntX(xz);
                int z = MathMan.unpairIntY(xz);
                List<IChunkAccess> l = new ArrayList<>((radius + 1 + radius) * (radius + 1 + radius));
                for (int zz = z - radius; zz <= z + radius; zz++) { //order is important, first z then x
                    for (int xx = x - radius; xx <= x + radius; xx++) {
                        l.add(protoChunks.get(MathMan.pairInt(xx, zz)));
                    }
                }
                map.put(xz, l);
            }
            worldlimits.put(radius, map);
        });

        //run generation tasks excluding FULL chunk status
        for (Map.Entry<ChunkStatus, Concurrency> entry : chunkStati.entrySet()) {
            ChunkStatus chunkStatus = entry.getKey();
            int radius = chunkStatus.requiredNeighborChunkRadius0();

            List<Long> coords = chunkCoordsForRadius.get(radius);
            if (this.generateConcurrent && entry.getValue() == Concurrency.RADIUS) {
                SequentialTasks<ConcurrentTasks<SequentialTasks<Long>>> tasks = getChunkStatusTaskRows(coords, radius);
                for (ConcurrentTasks<SequentialTasks<Long>> para : tasks) {
                    List<Runnable> scheduled = new ArrayList<>(tasks.size());
                    for (SequentialTasks<Long> row : para) {
                        scheduled.add(() -> {
                            for (Long xz : row) {
                                chunkStatus.processChunkSave(xz, worldlimits.get(radius).get(xz));
                            }
                        });
                    }
                    try {
                        List<Future<?>> futures = new ArrayList<>();
                        scheduled.forEach(task -> futures.add(executor.submit(task)));
                        for (Future<?> future : futures) {
                            future.get();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (this.generateConcurrent && entry.getValue() == Concurrency.FULL) {
                // every chunk can be processed individually
                List<Runnable> scheduled = new ArrayList<>(coords.size());
                for (long xz : coords) {
                    scheduled.add(() -> {
                        chunkStatus.processChunkSave(xz, worldlimits.get(radius).get(xz));
                    });
                }
                try {
                    List<Future<?>> futures = new ArrayList<>();
                    scheduled.forEach(task -> futures.add(executor.submit(task)));
                    for (Future<?> future : futures) {
                        future.get();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else { // Concurrency.NONE or generateConcurrent == false
                // run sequential but submit to different thread
                // running regen on the main thread otherwise triggers async-only events on the main thread
                executor.submit(() -> {
                    for (long xz : coords) {
                        chunkStatus.processChunkSave(xz, worldlimits.get(radius).get(xz));
                    }
                }).get(); // wait until finished this step
            }
        }

        //convert to proper chunks
        for (Long xz : chunkCoordsForRadius.get(0)) {
            ProtoChunk proto = protoChunks.get(xz);
            chunks.put(xz, createChunk(proto));
        }

        //final chunkstatus
        ChunkStatus FULL = getFullChunkStatus();
        for (Long xz : chunkCoordsForRadius.get(0)) { //FULL.requiredNeighbourChunkRadius() == 0!
            Chunk chunk = chunks.get(xz);
            FULL.processChunkSave(xz, Arrays.asList(chunk));
        }

        //populate
        List<BlockPopulator> populators = getBlockPopulators();
        for (Long xz : chunkCoordsForRadius.get(0)) {
            int x = MathMan.unpairIntX(xz);
            int z = MathMan.unpairIntY(xz);

            //prepare chunk seed
            Random random = getChunkRandom(seed, x, z);

            //actually populate
            Chunk c = chunks.get(xz);
            populators.forEach(pop -> {
                populate(c, random, pop);
            });
        }

        source = new SingleThreadQueueExtent(BukkitWorld.HAS_MIN_Y ? originalBukkitWorld.getMinHeight() : 0,
                BukkitWorld.HAS_MIN_Y ? originalBukkitWorld.getMaxHeight() : 256);
        source.init(target, initSourceQueueCache(), null);
        return true;
    }

    private void copyToWorld() {
        //Setting Blocks
        boolean genbiomes = options.shouldRegenBiomes();
        boolean hasBiome = options.hasBiomeType();
        BiomeType biome = options.getBiomeType();
        if (!genbiomes && !hasBiome) {
            target.setBlocks(region, new PlacementPattern());
        }
        if (hasBiome) {
            target.setBlocks(region, new WithBiomePlacementPattern(ignored -> biome));
        } else if (genbiomes) {
            target.setBlocks(region, new WithBiomePlacementPattern(vec -> source.getBiome(vec)));
        }
    }

    private class PlacementPattern implements Pattern {

        @Override
        public BaseBlock applyBlock(final BlockVector3 position) {
            return source.getFullBlock(position);
        }

        @Override
        public boolean apply(final Extent extent, final BlockVector3 get, final BlockVector3 set) throws WorldEditException {
            return extent.setBlock(set.getX(), set.getY(), set.getZ(), source.getFullBlock(get.getX(), get.getY(), get.getZ()));
        }

    }

    private class WithBiomePlacementPattern implements Pattern {

        private final Function<BlockVector3, BiomeType> biomeGetter;

        private WithBiomePlacementPattern(final Function<BlockVector3, BiomeType> biomeGetter) {
            this.biomeGetter = biomeGetter;
        }

        @Override
        public BaseBlock applyBlock(final BlockVector3 position) {
            return source.getFullBlock(position);
        }

        @Override
        public boolean apply(final Extent extent, final BlockVector3 get, final BlockVector3 set) throws WorldEditException {
            return extent.setBlock(set.getX(), set.getY(), set.getZ(), source.getFullBlock(get.getX(), get.getY(), get.getZ()))
                    && extent.setBiome(set.getX(), set.getY(), set.getZ(), biomeGetter.apply(get));
        }

    }

    //functions to be implemented by sub class
    private void cleanup0() {
        if (executor != null) {
            executor.shutdownNow();
        }
        cleanup();
    }

    /**
     * <p>Implement the preparation process in here. DO NOT instanciate any variable here that require the cleanup function. This function is for gathering further information before initializing a new
     * world.</p>
     *
     * <p>Fields required to be initialized: chunkStati, seed</p>
     * <p>For chunkStati also see {code ChunkStatusWrapper}.</p>
     *
     * @return whether or not the preparation process was successful
     */
    protected abstract boolean prepare();

    /**
     * Implement the creation of the seperate world in here.
     * <p>
     * Fields required to be initialized: generateConcurrent
     *
     * @return true if everything went fine, otherwise false. When false is returned the Regenerator halts the regeneration process and calls the cleanup function.
     * @throws java.lang.Exception When the implementation of this method throws and exception the Regenerator halts the regeneration process and calls the cleanup function.
     */
    protected abstract boolean initNewWorld() throws Exception;

    //functions to implement by sub class - regenate related

    /**
     * Implement the cleanup of all the mess that is created during the regeneration process (initNewWorld() and generate()).This function must not throw any exceptions.
     */
    protected abstract void cleanup();

    /**
     * Implement the initialization of a {@code ProtoChunk} here.
     *
     * @param x the x coorinate of the {@code ProtoChunk} to create
     * @param z the z coorinate of the {@code ProtoChunk} to create
     * @return an initialized {@code ProtoChunk}
     */
    protected abstract ProtoChunk createProtoChunk(int x, int z);

    /**
     * Implement the convertion of a {@code ProtoChunk} to a {@code Chunk} here.
     *
     * @param protoChunk the {@code ProtoChunk} to be converted to a {@code Chunk}
     * @return the converted {@code Chunk}
     */
    protected abstract Chunk createChunk(ProtoChunk protoChunk);

    /**
     * Return the {@code ChunkStatus.FULL} here.
     * ChunkStatus.FULL is the last step of vanilla chunk generation.
     *
     * @return {@code ChunkStatus.FULL}
     */
    protected abstract ChunkStatus getFullChunkStatus();

    /**
     * Return a list of {@code BlockPopulator} used to populate the original world here.
     *
     * @return {@code ChunkStatus.FULL}
     */
    protected abstract List<BlockPopulator> getBlockPopulators();

    /**
     * Implement the population of the {@code Chunk} with the given chunk random and {@code BlockPopulator} here.
     *
     * @param chunk  the {@code Chunk} to populate
     * @param random the chunk random to use for population
     * @param pop    the {@code BlockPopulator} to use
     */
    protected abstract void populate(Chunk chunk, Random random, BlockPopulator pop);

    /**
     * Implement the initialization an {@code IChunkCache<IChunkGet>} here. Use will need the {@code getChunkAt} function
     *
     * @return an initialized {@code IChunkCache<IChunkGet>}
     */
    protected abstract IChunkCache<IChunkGet> initSourceQueueCache();

    //algorithms
    private List<Long> getChunkCoordsRegen(Region region, int border) { //needs to be square num of chunks
        BlockVector3 oldMin = region.getMinimumPoint();
        BlockVector3 newMin = BlockVector3.at(
                (oldMin.getX() >> 4 << 4) - border * 16,
                oldMin.getY(),
                (oldMin.getZ() >> 4 << 4) - border * 16
        );
        BlockVector3 oldMax = region.getMaximumPoint();
        BlockVector3 newMax = BlockVector3.at(
                (oldMax.getX() >> 4 << 4) + (border + 1) * 16 - 1,
                oldMax.getY(),
                (oldMax.getZ() >> 4 << 4) + (border + 1) * 16 - 1
        );
        Region adjustedRegion = new CuboidRegion(newMin, newMax);
        return adjustedRegion.getChunks().stream()
                .sorted(Comparator
                        .comparingInt(BlockVector2::getZ)
                        .thenComparingInt(BlockVector2::getX)) //needed for RegionLimitedWorldAccess
                .map(c -> MathMan.pairInt(c.getX(), c.getZ()))
                .collect(Collectors.toList());
    }

    /**
     * Creates a list of chunkcoord rows that may be executed concurrently
     *
     * @param allcoords                   the coords that should be sorted into rows, must be sorted by z and x
     * @param requiredNeighborChunkRadius the radius of neighbor chunks that may not be written to concurrently (ChunkStatus
     *                                    .requiredNeighborRadius)
     * @return a list of chunkcoords rows that may be executed concurrently
     */
    private SequentialTasks<ConcurrentTasks<SequentialTasks<Long>>> getChunkStatusTaskRows(
            List<Long> allcoords,
            int requiredNeighborChunkRadius
    ) {
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
                for (int i = 0; minx + i * numlists + offset <= maxx; i++) {
                    para.add(byx.get(minx + i * numlists + offset));
                }
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
                for (int i = 0; minz + i * numlists + offset <= maxz; i++) {
                    para.add(byz.get(minz + i * numlists + offset));
                }
                tasks.add(para);
            }
        }

        return tasks;
    }

    protected BiomeProvider getBiomeProvider() {
        if (options.hasBiomeType()) {
            return new SingleBiomeProvider();
        }
        return originalBukkitWorld.getBiomeProvider();
    }

    //classes

    public enum Concurrency {
        FULL,
        RADIUS,
        NONE
    }

    /**
     * This class is used to wrap the ChunkStatus of the current Minecraft implementation and as the implementation to execute a chunk generation step.
     *
     * @param <IChunkAccess> the IChunkAccess class of the current Minecraft implementation
     */
    public static abstract class ChunkStatusWrapper<IChunkAccess> {

        /**
         * Return the required neighbor chunk radius the wrapped {@code ChunkStatus} requires.
         *
         * @return the radius of required neighbor chunks
         */
        public abstract int requiredNeighborChunkRadius();

        int requiredNeighborChunkRadius0() {
            return Math.max(0, requiredNeighborChunkRadius());
        }

        /**
         * Return the name of the wrapped {@code ChunkStatus}.
         *
         * @return the radius of required neighbor chunks
         */
        public abstract String name();

        /**
         * Return the name of the wrapped {@code ChunkStatus}.
         *
         * @param xz               represents the chunk coordinates of the chunk to process as denoted by {@code MathMan}
         * @param accessibleChunks a list of chunks that will be used during the execution of the wrapped {@code ChunkStatus}.
         *                         This list is order in the correct order required by the {@code ChunkStatus}, unless Mojang suddenly decides to do things differently.
         */
        public abstract CompletableFuture<?> processChunk(Long xz, List<IChunkAccess> accessibleChunks);

        void processChunkSave(Long xz, List<IChunkAccess> accessibleChunks) {
            try {
                processChunk(xz, accessibleChunks).get();
            } catch (Exception e) {
                LOGGER.error(
                        "Error while running " + name() + " on chunk " + MathMan.unpairIntX(xz) + "/" + MathMan.unpairIntY(xz),
                        e
                );
            }
        }

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
            return tasks.toString();
        }

    }

    public class SingleBiomeProvider extends BiomeProvider {

        private final org.bukkit.block.Biome biome = BukkitAdapter.adapt(options.getBiomeType());

        @Override
        public org.bukkit.block.Biome getBiome(final WorldInfo worldInfo, final int x, final int y, final int z) {
            return biome;
        }

        @Override
        public List<org.bukkit.block.Biome> getBiomes(final WorldInfo worldInfo) {
            return Collections.singletonList(biome);
        }

    }

}
