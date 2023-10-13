package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level;

import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * ChunkAccess which just delegates everything to an extent
 */
public class FaweProtoChunkAccess extends ProtoChunk {

    private final int hashedCoords;

    public FaweProtoChunkAccess(
            final FaweChunkSource source,
            final ChunkPos pos,
            final Registry<Biome> biomeRegistry,
            final ChunkStatus status,
            final RegenOptions options
    ) {
        super(
                pos, UpgradeData.EMPTY, null,
                NoopProtoChunkTick.BLOCKS, NoopProtoChunkTick.FLUIDS,
                source.getLevel(), biomeRegistry, null
        );
        for (int i = 0; i < getSections().length; i++) {
            getSections()[i] = new FaweProtoChunkSection(biomeRegistry, options);
        }
        setStatus(status);

        this.hashedCoords = Objects.hash(getPos().x, getPos().z);
    }

    @Override
    public @NotNull BlockState getBlockState(final int x, final int y, final int z) {
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        return getSection(getSectionIndex(y)).getBlockState(x & 15, y & 15, z & 15);
    }

    @Nullable
    @Override
    public BlockState setBlockState(final @NotNull BlockPos pos, final @NotNull BlockState state, final boolean moved) {
        if (this.isOutsideBuildHeight(pos)) {
            return null;
        }
        return getSection(getSectionIndex(pos.getY())).setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
    }

    @Override
    public void setBlockEntity(final @NotNull BlockEntity blockEntity) {
        if (this.isOutsideBuildHeight(blockEntity.getBlockPos())) {
            return;
        }
        super.setBlockEntity(blockEntity);
    }

    @Override
    public void addEntity(final @NotNull Entity entity) {
        if (this.isOutsideBuildHeight(entity.getBlockY())) {
            return;
        }
        super.addEntity(entity);
    }

    @Override
    public void removeBlockEntity(final @NotNull BlockPos pos) {
        if (this.isOutsideBuildHeight(pos.getY())) {
            return;
        }
        super.removeBlockEntity(pos);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof FaweProtoChunkAccess otherChunk)) {
            return false;
        }
        return otherChunk.getPos().equals(this.getPos());
    }

    @Override
    public int hashCode() {
        return this.hashedCoords;
    }

    @Override
    public String toString() {
        return "FaweProtoChunkAccess{" +
                "chunkPos=" + chunkPos +
                '}';
    }

}
