package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class NoiseWorker extends VersionedChunkWorker {

    public static final VersionedChunkWorker INSTANCE = new NoiseWorker();

    public NoiseWorker() {
        super(ChunkStatus.NOISE, true);
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
        return chunkGenerator.fillFromNoise(
                executor.get(),
                Blender.empty(),
                seededRandomState(level.chunkSource.randomState(), level, chunkGenerator, regenOptions),
                level.structureManager(),
                chunks.get(chunks.size() / 2)
        ).thenApply(chunkAccess -> true);
    }

}
