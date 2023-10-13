package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator;

import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level.FaweProtoChunkAccess;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DelegatingWorldGenRegion extends WorldGenRegion {

    private final RegenOptions regenOptions;

    public DelegatingWorldGenRegion(
            final ServerLevel world,
            final List<ChunkAccess> chunkAccesses,
            final ChunkStatus status,
            final int placementRadius,
            final RegenOptions regenOptions
    ) {
        super(world, chunkAccesses, status, placementRadius);
        this.regenOptions = regenOptions;
    }


    @Override
    public boolean ensureCanWrite(final BlockPos pos) {
        return this.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) &&
                pos.getY() >= getMinBuildHeight() && pos.getY() <= getMaxBuildHeight();
    }

    @Override
    public boolean isOldChunkAround(final @NotNull ChunkPos chunkPos, final int checkRadius) {
        return false; // We don't migrate old worlds, won't be properly blended either way
    }

    /**
     * Don't notify ServerLevel on place and don't set chunk for postprocessing
     */
    @Override
    public boolean setBlock(
            final @NotNull BlockPos pos,
            final @NotNull BlockState state,
            final int flags,
            final int maxUpdateDepth
    ) {
        if (!this.ensureCanWrite(pos)) {
            return false;
        }
        ChunkAccess chunk = this.getChunk(pos);
        BlockState oldState = chunk.setBlockState(pos, state, false);
        if (state.hasBlockEntity()) {
            BlockEntity tileEntity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            if (tileEntity != null) {
                chunk.setBlockEntity(tileEntity);
            } else {
                chunk.removeBlockEntity(pos);
            }
        } else if (oldState != null && oldState.hasBlockEntity()) {
            chunk.removeBlockEntity(pos);
        }
        return true;
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(final int biomeX, final int biomeY, final int biomeZ) {
        FaweProtoChunkAccess chunkAccess = (FaweProtoChunkAccess) this.getChunk(
                QuartPos.toSection(biomeX), QuartPos.toSection(biomeZ), ChunkStatus.EMPTY, false
        );
        if (chunkAccess == null) {
            return DedicatedServer.getServer().registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(Biomes.PLAINS);
        }
        int l = QuartPos.fromBlock(this.getMinBuildHeight());
        int i1 = l + QuartPos.fromBlock(this.getHeight()) - 1;
        int j1 = Mth.clamp(biomeY, l, i1);
        int k1 = this.getSectionIndex(QuartPos.toBlock(j1));
        return chunkAccess.getSection(k1).getNoiseBiome(biomeX & 3, j1 & 3, biomeZ & 3);
    }

    @Override
    public long getSeed() {
        return this.regenOptions.getSeed().orElse(super.getSeed());
    }

}
