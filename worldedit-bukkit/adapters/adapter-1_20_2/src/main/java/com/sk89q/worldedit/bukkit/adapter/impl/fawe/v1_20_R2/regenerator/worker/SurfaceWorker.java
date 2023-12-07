package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class SurfaceWorker extends VersionedChunkWorker {

    public static final VersionedChunkWorker INSTANCE = new SurfaceWorker();

    public SurfaceWorker() {
        super(ChunkStatus.SURFACE, false);
    }

    @Override
    public CompletableFuture<Boolean> work(
            final ServerLevel level,
            final List<ChunkAccess> chunks,
            final WorldGenRegion worldGenRegion,
            final ChunkGenerator chunkGenerator,
            final RegenOptions regenOptions,
            final Region region,
            final Supplier<Executor> executor
    ) {
        return CompletableFuture.runAsync(() -> chunkGenerator.buildSurface(
                worldGenRegion, level.structureManager(),
                seededRandomState(level.chunkSource.randomState(), level, chunkGenerator, regenOptions),
                chunks.get(chunks.size() / 2)
        ), executor.get()).thenApply(unused -> true);
    }

}
