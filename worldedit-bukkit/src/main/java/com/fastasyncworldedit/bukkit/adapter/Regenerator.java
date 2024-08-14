package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.SingleThreadQueueExtent;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.Logger;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an abstract regeneration handler.
 *
 * @param <IChunkAccess> the type of the {@code IChunkAccess} of the current Minecraft implementation
 * @param <ProtoChunk>   the type of the {@code ProtoChunk} of the current Minecraft implementation
 * @param <Chunk>        the type of the {@code Chunk} of the current Minecraft implementation
 * @param <ChunkStatus>  the type of the {@code ChunkStatusWrapper} wrapping the {@code ChunkStatus} enum
 */
public abstract class Regenerator<IChunkAccess, ProtoChunk extends IChunkAccess, Chunk extends IChunkAccess, ChunkStatus> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected final org.bukkit.World originalBukkitWorld;
    protected final Region region;
    protected final Extent target;
    protected final RegenOptions options;

    //runtime
    protected final LinkedHashMap<ChunkStatus, Concurrency> chunkStatuses = new LinkedHashMap<>(); // TODO (j21): use SequencedMap
    private final Long2ObjectLinkedOpenHashMap<ProtoChunk> protoChunks = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    protected boolean generateConcurrent = true;
    protected long seed;
    protected SingleThreadQueueExtent source;

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

    /**
     * Regenerates the selected {@code Region}.
     *
     * @return whether the regeneration process was successful
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

    private void createSource() {

        source = new SingleThreadQueueExtent(
                BukkitWorld.HAS_MIN_Y ? originalBukkitWorld.getMinHeight() : 0,
                BukkitWorld.HAS_MIN_Y ? originalBukkitWorld.getMaxHeight() : 256
        );
        source.init(target, initSourceQueueCache(), null);
    }

    private void copyToWorld() {
        createSource();
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
            return extent.setBlock(set.x(), set.y(), set.z(), source.getFullBlock(get.x(), get.y(), get.z()));
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
            return extent.setBlock(set.x(), set.y(), set.z(), source.getFullBlock(get.x(), get.y(), get.z()))
                    && extent.setBiome(set.x(), set.y(), set.z(), biomeGetter.apply(get));
        }

    }

    //functions to be implemented by sub class
    private void cleanup0() {
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
     * Implement the initialization an {@code IChunkCache<IChunkGet>} here. Use will need the {@code getChunkAt} function
     *
     * @return an initialized {@code IChunkCache<IChunkGet>}
     */
    protected abstract IChunkCache<IChunkGet> initSourceQueueCache();

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
         * @param accessibleChunks a list of chunks that will be used during the execution of the wrapped {@code ChunkStatus}.
         *                         This list is order in the correct order required by the {@code ChunkStatus}, unless Mojang suddenly decides to do things differently.
         */
        public abstract CompletableFuture<?> processChunk(List<IChunkAccess> accessibleChunks);

        void processChunkSave(long xz, List<IChunkAccess> accessibleChunks) {
            try {
                processChunk(accessibleChunks).get();
            } catch (Exception e) {
                LOGGER.error("Error while running {} on chunk {}/{}",
                        name(), MathMan.unpairIntX(xz), MathMan.unpairIntY(xz), e);
            }
        }

        @Override
        public String toString() {
            return name();
        }

    }

    public static class SequentialTasks<T> extends Tasks<T> {

        public SequentialTasks(int expectedSize) {
            super(expectedSize);
        }

    }

    public static class ConcurrentTasks<T> extends Tasks<T> {

        public ConcurrentTasks(int expectedSize) {
            super(expectedSize);
        }

    }

    public static class Tasks<T> implements Iterable<T> {

        private final List<T> tasks;

        public Tasks(int expectedSize) {
            tasks = new ArrayList<>(expectedSize);
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
