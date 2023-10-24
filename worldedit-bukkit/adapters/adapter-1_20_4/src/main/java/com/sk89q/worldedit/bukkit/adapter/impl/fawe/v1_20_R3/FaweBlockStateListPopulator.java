package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.craftbukkit.v1_20_R3.util.BlockStateListPopulator;
import org.jetbrains.annotations.Nullable;

public class FaweBlockStateListPopulator extends BlockStateListPopulator {

    private final ServerLevel world;

    public FaweBlockStateListPopulator(ServerLevel world) {
        super(world);
        this.world = world;
    }

    @Override
    public long getSeed() {
        return world.getSeed();
    }

    @Override
    public ServerLevel getLevel() {
        return world.getLevel();
    }

    @Override
    public MinecraftServer getServer() {
        return world.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return world.getChunkSource();
    }

    @Override
    public ChunkAccess getChunk(final int chunkX, final int chunkZ, final ChunkStatus leastStatus, final boolean create) {
        return world.getChunk(chunkX, chunkZ, leastStatus, create);
    }

    @Override
    public BiomeManager getBiomeManager() {
        return world.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(final int biomeX, final int biomeY, final int biomeZ) {
        return world.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return world.enabledFeatures();
    }

    @Override
    public float getShade(final Direction direction, final boolean shaded) {
        return world.getShade(direction, shaded);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return world.getLightEngine();
    }

    @Nullable
    @Override
    public ChunkAccess getChunkIfLoadedImmediately(final int x, final int z) {
        return world.getChunkIfLoadedImmediately(x, z);
    }

    @Override
    public BlockState getBlockStateIfLoaded(final BlockPos blockposition) {
        return world.getBlockStateIfLoaded(blockposition);
    }

    @Override
    public FluidState getFluidIfLoaded(final BlockPos blockposition) {
        return world.getFluidIfLoaded(blockposition);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return world.getWorldBorder();
    }

}
