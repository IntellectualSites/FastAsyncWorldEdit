package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2;

import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.enginehub.linbus.tree.LinCompoundTag;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PaperweightLevelProxy extends ServerLevel {

    protected ServerLevel serverLevel;
    private PaperweightFaweAdapter adapter;
    private PaperweightPlacementStateProcessor processor;

    @SuppressWarnings("DataFlowIssue")
    private PaperweightLevelProxy() {
        super(null, null, null, null, null, null, null, true, 0L, null, true, null, null, null, null);
        throw new IllegalStateException("Cannot be instantiated");
    }

    public static PaperweightLevelProxy getInstance(ServerLevel serverLevel, PaperweightPlacementStateProcessor processor) {
        Unsafe unsafe = ReflectionUtils.getUnsafe();

        PaperweightLevelProxy newLevel;
        try {
            newLevel = (PaperweightLevelProxy) unsafe.allocateInstance(PaperweightLevelProxy.class);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        newLevel.processor = processor;
        newLevel.adapter = ((PaperweightFaweAdapter) WorldEditPlugin.getInstance().getBukkitImplAdapter());
        newLevel.serverLevel = serverLevel;
        return newLevel;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@Nonnull BlockPos blockPos) {
        if (blockPos.getX() == Integer.MAX_VALUE) {
            return null;
        }
        LinCompoundTag tag = processor.getTileAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        if (tag == null) {
            return null;
        }
        BlockState state = adapter.adapt(processor.getBlockStateAt(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }
        BlockEntity tileEntity = entityBlock.newBlockEntity(blockPos, state);
        tileEntity.load((CompoundTag) adapter.fromNativeLin(tag));
        return tileEntity;
    }

    @Override
    @Nonnull
    public BlockState getBlockState(@Nonnull BlockPos blockPos) {
        if (blockPos.getX() == Integer.MAX_VALUE) {
            return Blocks.AIR.defaultBlockState();
        }
        com.sk89q.worldedit.world.block.BlockState state = processor.getBlockStateAt(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ()
        );
        return adapter.adapt(state);
    }

    @SuppressWarnings("unused")
    @Override
    @Nonnull
    public FluidState getFluidState(@Nonnull BlockPos pos) {
        if (pos.getX() == Integer.MAX_VALUE) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return getBlockState(pos).getFluidState();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean isWaterAt(@Nonnull BlockPos pos) {
        if (pos.getX() == Integer.MAX_VALUE) {
            return false;
        }
        return getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

    @Override
    public int getHeight() {
        return serverLevel.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return serverLevel.getMinBuildHeight();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return serverLevel.getWorldBorder();
    }

}
