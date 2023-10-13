package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FaweBlockGetter implements BlockGetter {

    private final FaweChunkSource chunkSource;

    public FaweBlockGetter(final FaweChunkSource chunkSource) {
        this.chunkSource = chunkSource;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(final BlockPos pos) {
        final ChunkAccess chunkAccess = this.chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        return chunkAccess == null ? null : chunkAccess.getBlockEntity(pos);
    }

    @Override
    public @NotNull BlockState getBlockState(final BlockPos pos) {
        final ChunkAccess chunkAccess = this.chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        return chunkAccess == null ? Blocks.VOID_AIR.defaultBlockState() : chunkAccess.getBlockState(pos);
    }

    @Nullable
    @Override
    public BlockState getBlockStateIfLoaded(final @NotNull BlockPos pos) {
        final ChunkAccess chunkAccess = this.chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        return chunkAccess == null ? Blocks.VOID_AIR.defaultBlockState() : chunkAccess.getBlockStateIfLoaded(pos);
    }

    @Nullable
    @Override
    public FluidState getFluidIfLoaded(final @NotNull BlockPos pos) {
        final ChunkAccess chunkAccess = this.chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        return chunkAccess == null ? null : chunkAccess.getFluidIfLoaded(pos);
    }

    @Override
    public @NotNull FluidState getFluidState(final @NotNull BlockPos pos) {
        final ChunkAccess chunkAccess = this.chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        return chunkAccess == null ? Fluids.EMPTY.defaultFluidState() : chunkAccess.getFluidState(pos);
    }

    @Override
    public int getHeight() {
        return this.chunkSource.levelHeightAccessor().getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return this.chunkSource.levelHeightAccessor().getMinBuildHeight();
    }

}
