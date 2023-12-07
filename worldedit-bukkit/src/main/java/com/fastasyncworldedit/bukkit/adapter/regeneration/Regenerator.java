package com.fastasyncworldedit.bukkit.adapter.regeneration;

import com.fastasyncworldedit.bukkit.adapter.regeneration.queue.QueuedRegenerationSection;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Abstraction of World-Regeneration logic. Contains boilerplate code for faster versioned implementation.
 * <p>
 * Regeneration happens in batches of 4 chunks at a time to reduce memory overhead: When regenerating chunks some generation
 * steps require adjacent chunks to be loaded and populated as well - whereas the maximum radius is 8. Therefor for each chunk
 * generating, {@code 289} chunks have to be kept in memory each. Adjacent chunks may not be shared due to parallelism and
 * potential different states and differing {@link ChunkStatus} which will lead to generation failure.
 *
 * @param <ServerLevel>    The native (versioned) ServerLevel
 * @param <WorldGenRegion> The native (versioned) ServerLevel
 * @param <ChunkAccess>    The native (versioned) ChunkAccess
 * @param <ChunkGenerator> The native (versioned) ChunkGenerator (NMS, not CB)
 * @param <ChunkStatus>    The native (versioned) ChunkStatus
 */
public abstract class Regenerator<ServerLevel, WorldGenRegion, ChunkAccess, ChunkGenerator, ChunkStatus>
        implements Closeable {

    public static final int TASK_MARGIN = 8;
    protected static final int[] PLACEMENT_RADII = new int[]{-1, 0, 0, 0, 1};
    private static final short QUEUE_LIMIT = 4;
    private static final int PER_CHUNK_CACHE_SIZE = (TASK_MARGIN + TASK_MARGIN + 1) * (TASK_MARGIN + TASK_MARGIN + 1);

    protected final World world;
    protected final Region region;
    protected final RegenOptions options;
    protected final Extent extent;

    private final Queue<QueuedRegenerationSection> queue;

    /**
     * Instantiates a new Regeneration-Context for single-use only.
     *
     * @param world   The world containing the regeneration selection.
     * @param region  The selected region by the user to be regenerated.
     * @param options The possible options as passed by the api or command parameters.
     * @param extent  The outgoing extent to where the regenerated blocks shall be flushed.
     */
    protected Regenerator(final World world, final Region region, final RegenOptions options, final Extent extent) {
        this.world = world;
        this.region = region;
        this.options = options;
        this.extent = extent;
        this.queue = new ArrayDeque<>((int) Math.ceil((double) region.getChunks().size() / QUEUE_LIMIT));
        this.populateQueue();
    }

    /**
     * Simply triggers the initial {@link #poll()} which will also recursively trigger all other chunks.
     *
     * @return {@code true} if all chunks fully regenerated inside the selection.
     */
    public CompletableFuture<Boolean> regenerate() {
        return this.poll();
    }

    /**
     * Regenerates a single chunk by populating the squared list containing the target chunk and their required adjacent chunks.
     * Triggers {@link #runWorker(int, List)} with the index {@code 0} to start the worker chain.
     *
     * @param chunk The chunk position (absolute) to be regenerated.
     * @return The Future completing when the chunk is regenerated.
     * @see #runWorker(int, List)
     */
    public CompletableFuture<Boolean> regenerateChunk(BlockVector2 chunk) {
        final ServerLevel level = serverLevel();
        // Has to be ordered, as generating chunks are often get by the index: size(list) / 2
        final List<ChunkAccess> accesses = new ArrayList<>(PER_CHUNK_CACHE_SIZE);
        for (int x = chunk.getX() - TASK_MARGIN; x <= chunk.getX() + TASK_MARGIN; x++) {
            for (int z = chunk.getZ() - TASK_MARGIN; z <= chunk.getZ() + TASK_MARGIN; z++) {
                accesses.add(toNativeChunkAccess(level, chunk, x, z, workers().get(0).previousStatus()));
            }
        }
        return runWorker(0, accesses);
    }


    /**
     * Polls the next {@link QueuedRegenerationSection} to be regenerated. If the queue is empty after
     * {@link Queue#poll() polling} the next {@link QueuedRegenerationSection}, no further calls to this method are
     * allowed.
     * <br><br>
     * Automatically calls this method again after regeneration the polled {@link QueuedRegenerationSection} if the queue
     * contains further items. Their results are combined.
     *
     * @return A Future resolving when the polled {@link QueuedRegenerationSection} has finished regenerating.
     *         Contains {@code true} if <b>every section</b> has regenerated successfully. Otherwise {@code false}.
     */
    private @NonNull CompletableFuture<@NonNull Boolean> poll() {
        final QueuedRegenerationSection section = this.queue.poll();
        if (section == null) {
            throw new IllegalStateException("Attempted to call #poll() while Queue is empty");
        }
        CompletableFuture<Boolean> future = section.regenerate();
        if (this.queue.peek() != null) {
            // Basically, regenerate the polled section, then:
            // If successful, call this method again after regeneration and return its result (as that would be the logical
            // combination either way, as we know this step was successful)
            // If failed, return false and don't continue regenerating. Nobody want's a half-cooked chicken.
            future = future.thenCompose(thisResult -> thisResult ? poll() : CompletableFuture.completedFuture(false));
        }
        return future;
    }

    /**
     * Executes a {@link ChunkWorker} based on their index by accessing {@link #workers()}.
     * A {@link ChunkWorker} represents a generation step as defined in {@link ChunkStatus} as well.
     *
     * @param workerIndex   The index of the {@link ChunkWorker} in the list to execute.
     * @param chunkAccesses The list of {@link ChunkAccess ChunkAccesses} for regeneration.
     * @return A future resolving when the generation step finished containing its result (success / failed).
     */
    private CompletableFuture<Boolean> runWorker(
            final int workerIndex,
            final List<ChunkAccess> chunkAccesses
    ) {
        final ServerLevel level = serverLevel();
        final ChunkAccess center = chunkAccesses.get(chunkAccesses.size() / 2);
        final ChunkWorker<ServerLevel, ChunkAccess, WorldGenRegion, ChunkGenerator, ChunkStatus> worker =
                workers().get(workerIndex);
        final WorldGenRegion worldGenRegion = worldGenRegionForRegion(level, chunkAccesses, region, worker.status());

        CompletableFuture<Boolean> result = worker.work(
                level, chunkAccesses, worldGenRegion, generatorFromLevel(level), this.options, this.region, provideExecutor()
        ).thenCompose(success -> {
            // The ChunkStatus must always be set to the previously (finished) generation step after finished.
            setChunkStatus(chunkAccesses, worker.status());
            // If this worker step was successful, do post-processing (like priming heightmaps)
            if (success) {
                return postProcessChunkGeneration(worker, center).thenApply(unused -> true);
            }
            return CompletableFuture.completedFuture(false);
        });
        // If we finished the last worker step, flush all changes into the world (EditSession) and complete the future
        if (workerIndex >= workers().size() - 1) {
            return result.thenCompose(success -> success ? flushPalettesIntoWorld(center, worldGenRegion) :
                    CompletableFuture.completedFuture(false));
        }
        // If there are still more worker steps, enqueue the next one (if the current step finished successfully)
        return result.thenCompose(success -> {
            if (success) {
                return runWorker(workerIndex + 1, chunkAccesses);
            }
            throw new CompletionException(
                    "Cant enqueue next worker",
                    new RuntimeException("Worker " + worker.status() + " returned non-true result for " + center)
            );
        });
    }

    /**
     * Optional post-processing of a chunk after finishing each {@link ChunkWorker}.
     * Currently only re-calculates heightmaps for the next {@link ChunkWorker} if
     * {@link ChunkWorker#shouldPrimeHeightmapsAfter()} is {@code true}
     *
     * @param worker      The recently finished {@link ChunkWorker}.
     * @param chunkAccess The target {@link ChunkAccess} which was populated using the worker.
     * @return A future resolving {@code null} when the heightmap calculation finished.
     */
    private CompletableFuture<Void> postProcessChunkGeneration(
            ChunkWorker<ServerLevel, ChunkAccess, WorldGenRegion, ChunkGenerator, ChunkStatus> worker,
            ChunkAccess chunkAccess
    ) {
        if (worker.shouldPrimeHeightmapsAfter()) {
            return primeHeightmaps(chunkAccess);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Divides the Chunks inside the provided {@link Region} into smaller batches as limited by {@link #QUEUE_LIMIT}.
     */
    private void populateQueue() {
        final BlockVector2[] chunks = this.region.getChunks().toArray(BlockVector2[]::new);
        // Sort, so that adjacent chunks are generated in batches
        Arrays.sort(chunks, Comparator.comparingInt(BlockVector2::getX).thenComparing(BlockVector2::getZ));
        final int queued = (int) Math.ceil((double) chunks.length / QUEUE_LIMIT);
        final int divisor = chunks.length % QUEUE_LIMIT;
        for (int i = 0; i < queued; i++) {
            final boolean last = i == queued - 1;
            final int start = i * QUEUE_LIMIT;
            final int end = start + (last && divisor != 0 ? divisor : QUEUE_LIMIT);
            this.queue.add(new QueuedRegenerationSection(this, Arrays.copyOfRange(chunks, start, end)));
        }
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * May be overridden in version specific code, if required.
     *
     * @return A supplier providing an executor for asynchronous tasks.
     */
    public @NonNull Supplier<@NonNull Executor> provideExecutor() {
        return () -> task -> TaskManager.taskManager().async(task);
    }

    /**
     * Places all blocks, block entities and biomes into the actual work by accessing the block- and biome-palettes of the
     * chunk and setting their native contents into the {@link #extent passed Extent}.
     *
     * @param chunkAccess    The fully-regenerated chunk populated with blocks and biomes.
     * @param worldGenRegion The associated {@link WorldGenRegion} to provide access to biomes.
     * @return A future resolving when all changes are flushed containing its result (success / failure).
     */
    public abstract CompletableFuture<Boolean> flushPalettesIntoWorld(
            final ChunkAccess chunkAccess, final WorldGenRegion worldGenRegion
    );

    /**
     * Attempts to either create a new native {@link ChunkAccess} or get a cached version of an already created one.
     * If the cached {@link ChunkAccess} current {@link ChunkStatus} is less than {@code leastStatus} (as defined in the
     * ordinal order in native code) it's expected to return {@code null}.
     * <br>
     * This method <b>must</b> return or create a chunk for out-of-bounds chunks (as defined by the selected {@link #region})
     * as those are created for the required placement radii of certain generation stages. These {@link ChunkAccess accesses
     * } shall <b>not flush or set their changes</b> into the {@link #extent outgoing Extent}.
     *
     * @param level        The level containing the chunk (based on {@link #world} and {@link #serverLevel()}).
     * @param contextChunk The working context for which the chunk should be converted (= working chunk coordinate).
     * @param x            The x coordinate of the native {@link ChunkAccess}.
     * @param z            The z coordinate of the native {@link ChunkAccess}.
     * @param leastStatus  The current status of the chunk to be set.
     * @return The cached {@link ChunkAccess} if existing and applicable status, otherwise created or {@code null}.
     */
    protected abstract @Nullable ChunkAccess toNativeChunkAccess(
            final ServerLevel level, final BlockVector2 contextChunk, final int x, final int z, final ChunkStatus leastStatus
    );

    /**
     * Prime (recalculate) heightmaps of noised {@link ChunkAccess ChunkAccesses}.
     * This method must process all required heightmaps required by following steps.
     * Results must be written back into the native {@link ChunkAccess} for access by following generation stages. (Extents
     * don't have any functionality to store those results).
     *
     * @param chunk The {@link ChunkAccess} to prime the heightmaps for.
     * @return A Future resolving when the priming / calculation finished.
     */
    protected abstract @NonNull CompletableFuture<@Nullable Void> primeHeightmaps(ChunkAccess chunk);

    /**
     * Create a new native {@link WorldGenRegion} for the passed {@link ChunkStatus} (as those differ in {@code placement
     * radii} spanning the passed {@link Region}.
     *
     * @param level         The {@link ServerLevel} to be wrapped.
     * @param chunkAccesses The containing {@link ChunkAccess ChunkAccesses} for the {@link WorldGenRegion}.
     * @param region        The {@link Region} to be contained by the {@link WorldGenRegion}.
     * @param status        The {@link ChunkStatus} for the {@link WorldGenRegion} to be created.
     * @return The new {@link WorldGenRegion}.
     */
    protected abstract WorldGenRegion worldGenRegionForRegion(
            final ServerLevel level,
            final List<ChunkAccess> chunkAccesses,
            final Region region,
            final ChunkStatus status
    );

    /**
     * Getter-Abstraction to retrieve the native {@link ChunkGenerator} from a {@link ServerLevel}.
     *
     * @param level The {@link ServerLevel} to get the {@link ChunkGenerator} from.
     * @return The original {@link ChunkGenerator} of the {@link ServerLevel}.
     */
    protected abstract ChunkGenerator generatorFromLevel(ServerLevel level);

    /**
     * Set's the {@link ChunkStatus} for all {@link ChunkAccess ChunkAccesses} after finishing a generation step.
     *
     * @param chunkAccesses The {@link ChunkAccess ChunkAccesses} to be set the new {@link ChunkStatus}
     * @param status        The {@link ChunkStatus} by the recently finished {@link ChunkWorker} ({@link ChunkWorker#status()})
     */
    protected abstract void setChunkStatus(List<ChunkAccess> chunkAccesses, ChunkStatus status);

    /**
     * A list of all versioned {@link ChunkWorker ChunkWorkers} to fully regenerate the selection. Must be in order.
     *
     * @return All {@link ChunkWorker ChunkWorkers}.
     */
    protected abstract @NonNull List<@NonNull ChunkWorker<ServerLevel, ChunkAccess, WorldGenRegion, ChunkGenerator, ChunkStatus>> workers();

    /**
     * Getter-Abstraction to retrieve the native {@link ServerLevel} from the passed {@link #world World}.
     *
     * @return The native assigned {@link ServerLevel} for the {@link World Bukkit World}
     */
    protected abstract @NonNull ServerLevel serverLevel();

}
