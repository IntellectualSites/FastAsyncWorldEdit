package com.fastasyncworldedit.bukkit.adapter.regeneration;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A ChunkWorker represents a single regeneration step in a similar fashion as native ChunkStatuses do.
 *
 * @param <ServerLevel>    The native (versioned) ServerLevel
 * @param <WorldGenRegion> The native (versioned) ServerLevel
 * @param <ChunkAccess>    The native (versioned) ChunkAccess
 * @param <ChunkGenerator> The native (versioned) ChunkGenerator (NMS, not CB)
 * @param <ChunkStatus>    The native (versioned) ChunkStatus
 */
public abstract class ChunkWorker<ServerLevel, ChunkAccess, WorldGenRegion, ChunkGenerator, ChunkStatus> {

    /**
     * Do the actual work by calling the native code for generation this specific step.
     *
     * @param level          The level being assigned to the {@link ChunkAccess} for accessing original data like seeds.
     * @param chunks         The {@link ChunkAccess ChunkAccesses} to be worked on.
     * @param worldGenRegion The assigned {@link WorldGenRegion} for the {@code chunks} and this specific step.
     * @param chunkGenerator The original {@link ChunkGenerator} assigned to the {@link ServerLevel level}.
     * @param regenOptions   The possible options as passed by the api or command parameters.
     * @param region         The selected region by the user to be regenerated.
     * @param executor       The executor used to schedule asynchronous tasks.
     * @return A future resolving when this step has finished containing its result (success / failure)
     */
    public abstract CompletableFuture<Boolean> work(
            final ServerLevel level,
            final List<ChunkAccess> chunks,
            final WorldGenRegion worldGenRegion,
            final ChunkGenerator chunkGenerator,
            final RegenOptions regenOptions,
            final Region region,
            final Supplier<Executor> executor
    );

    /**
     * @return The native {@link ChunkStatus} assigned to this generation step.
     */
    public abstract ChunkStatus status();

    /**
     * @return The previous {@link ChunkStatus} as defined by {@link #status()}.
     */
    public abstract ChunkStatus previousStatus();

    /**
     * @return The following {@link ChunkStatus} as defined by {@link #status()}.
     */
    public abstract ChunkStatus nextStatus();

    /**
     * @return {@code true} if heightmaps shall be primed after this step has finished.
     */
    public abstract boolean shouldPrimeHeightmapsAfter();

}
