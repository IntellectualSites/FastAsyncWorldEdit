package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker;

import com.fastasyncworldedit.bukkit.adapter.regeneration.ChunkWorker;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

public abstract class VersionedChunkWorker extends ChunkWorker<ServerLevel, ChunkAccess, WorldGenRegion, ChunkGenerator,
        ChunkStatus> {

    private final ChunkStatus status;
    private final boolean primeHeightmapsAfter;

    public VersionedChunkWorker(final ChunkStatus status, final boolean primeHeightmapsAfter) {
        this.status = status;
        this.primeHeightmapsAfter = primeHeightmapsAfter;
    }

    protected RandomState seededRandomState(
            final RandomState originalState,
            final ServerLevel level,
            final ChunkGenerator chunkGenerator,
            final RegenOptions regenOptions
    ) {
        return regenOptions.getSeed().stream().mapToObj(value -> {
            if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
                return RandomState.create(
                        noiseBasedChunkGenerator.generatorSettings().value(),
                        level.registryAccess().lookupOrThrow(Registries.NOISE),
                        value
                );
            }
            return RandomState.create(
                    NoiseGeneratorSettings.dummy(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE),
                    value
            );
        }).findFirst().orElse(originalState);
    }

    @Override
    public ChunkStatus status() {
        return this.status;
    }

    @Override
    public ChunkStatus previousStatus() {
        return status.getParent();
    }

    @Override
    public ChunkStatus nextStatus() {
        return status.getNextStatus();
    }

    @Override
    public boolean shouldPrimeHeightmapsAfter() {
        return this.primeHeightmapsAfter;
    }

}
