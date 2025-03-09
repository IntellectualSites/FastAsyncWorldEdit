package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_R1;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.craftbukkit.util.BlockStateListPopulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    @Nonnull
    public ServerLevel getLevel() {
        return world.getLevel();
    }

    @Override
    @Nonnull
    public DifficultyInstance getCurrentDifficultyAt(final BlockPos pos) {
        return world.getCurrentDifficultyAt(pos);
    }

    @Override
    public MinecraftServer getServer() {
        return world.getServer();
    }

    @Override
    @Nonnull
    public ChunkSource getChunkSource() {
        return world.getChunkSource();
    }

    @Override
    @Nonnull
    public RandomSource getRandom() {
        return world.getRandom();
    }

    @Override
    public ChunkAccess getChunk(final int chunkX, final int chunkZ, final ChunkStatus leastStatus, final boolean create) {
        return world.getChunk(chunkX, chunkZ, leastStatus, create);
    }

    @Override
    @Nonnull
    public BiomeManager getBiomeManager() {
        return world.getBiomeManager();
    }

    @Override
    @Nonnull
    public Holder<Biome> getUncachedNoiseBiome(final int biomeX, final int biomeY, final int biomeZ) {
        return world.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    @Nonnull
    public FeatureFlagSet enabledFeatures() {
        return world.enabledFeatures();
    }

    @Override
    public float getShade(final Direction direction, final boolean shaded) {
        return world.getShade(direction, shaded);
    }

    @Override
    @Nonnull
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
    @Nonnull
    public WorldBorder getWorldBorder() {
        return world.getWorldBorder();
    }

    @Override
    public boolean setBlock(final BlockPos pos, final BlockState state, final int flags, final int maxUpdateDepth) {
        return world.setBlock(pos, state, flags, maxUpdateDepth);
    }

    @Override
    public boolean removeBlock(final BlockPos pos, final boolean move) {
        return world.removeBlock(pos, move);
    }

    @Override
    public boolean destroyBlock(final BlockPos pos, final boolean drop, final Entity breakingEntity, final int maxUpdateDepth) {
        return world.destroyBlock(pos, drop, breakingEntity, maxUpdateDepth);
    }

    @Override
    @Nonnull
    public BlockState getBlockState(final BlockPos pos) {
        return world.getBlockState(pos);
    }

    @Override
    public boolean setBlock(final BlockPos pos, final BlockState state, final int flags) {
        return world.setBlock(pos, state, flags);
    }

}
