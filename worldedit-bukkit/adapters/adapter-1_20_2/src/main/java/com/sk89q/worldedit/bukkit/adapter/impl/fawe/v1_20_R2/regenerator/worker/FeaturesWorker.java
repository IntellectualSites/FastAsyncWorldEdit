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

public class FeaturesWorker extends VersionedChunkWorker {

    public static final VersionedChunkWorker INSTANCE = new FeaturesWorker();

    public FeaturesWorker() {
        super(ChunkStatus.FEATURES, false);
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
        return CompletableFuture.runAsync(() -> chunkGenerator.applyBiomeDecoration(
                worldGenRegion, chunks.get(chunks.size() / 2), level.structureManager()
        ), executor.get()).thenApply(unused -> true);
    }

}
