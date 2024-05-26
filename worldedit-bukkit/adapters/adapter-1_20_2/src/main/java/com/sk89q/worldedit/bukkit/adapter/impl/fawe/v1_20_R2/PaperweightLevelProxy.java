package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2;

import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.Extent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PaperweightLevelProxy extends ServerLevel {

    private PaperweightFaweAdapter adapter;
    private Extent extent;
    private ServerLevel serverLevel;
    private boolean enabled = false;

    @SuppressWarnings("DataFlowIssue")
    public PaperweightLevelProxy() {
        super(null, null, null, null, null, null, null, true, 0L, null, true, null, null, null, null);
        throw new IllegalStateException("Cannot be instantiated");
    }

    public static PaperweightLevelProxy getInstance(ServerLevel level, Extent extent) {
        Unsafe unsafe = ReflectionUtils.getUnsafe();

        PaperweightLevelProxy newLevel;
        try {
            newLevel = (PaperweightLevelProxy) unsafe.allocateInstance(PaperweightLevelProxy.class);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        newLevel.adapter = ((PaperweightFaweAdapter) WorldEditPlugin
                .getInstance()
                .getBukkitImplAdapter());
        newLevel.extent = extent;
        newLevel.serverLevel = level;
        return newLevel;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@Nonnull BlockPos blockPos) {
        if (!enabled) {
            return null;
        }
        BlockEntity tileEntity = this.serverLevel.getChunkAt(blockPos).getBlockEntity(blockPos);
        if (tileEntity == null) {
            return null;
        }
        BlockEntity newEntity = tileEntity.getType().create(blockPos, getBlockState(blockPos));
        newEntity.load((CompoundTag) adapter.fromNativeLin(this.extent.getFullBlock(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ()
        ).getNbtReference().getValue()));

        return newEntity;
    }

    @Override
    @Nonnull
    public BlockState getBlockState(@Nonnull BlockPos blockPos) {
        if (!enabled) {
            return Blocks.AIR.defaultBlockState();
        }
        com.sk89q.worldedit.world.block.BlockState state = this.extent.getBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        return adapter.adapt(state);
    }

    @SuppressWarnings("unused")
    @Override
    @Nonnull
    public FluidState getFluidState(@Nonnull BlockPos pos) {
        if (!enabled) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return getBlockState(pos).getFluidState();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean isWaterAt(@Nonnull BlockPos pos) {
        if (!enabled) {
            return false;
        }
        return getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

}
