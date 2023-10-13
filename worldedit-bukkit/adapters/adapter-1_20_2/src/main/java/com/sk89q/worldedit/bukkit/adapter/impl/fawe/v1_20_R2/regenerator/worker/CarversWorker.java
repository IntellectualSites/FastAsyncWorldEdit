package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class CarversWorker extends VersionedChunkWorker {

    public static final VersionedChunkWorker INSTANCE = new CarversWorker();

    public CarversWorker() {
        super(ChunkStatus.CARVERS, true);
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
        return CompletableFuture.runAsync(() -> chunkGenerator.applyCarvers(
                worldGenRegion, regenOptions.getSeed().orElse(level.getSeed()),
                seededRandomState(level.chunkSource.randomState(), level, chunkGenerator, regenOptions),
                level.getBiomeManager(), level.structureManager(), chunks.get(chunks.size() / 2), GenerationStep.Carving.AIR
        ), executor.get()).thenApply(unused -> true);
    }

}
