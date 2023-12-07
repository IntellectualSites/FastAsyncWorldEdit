package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class NoopProtoChunkTick<T> extends ProtoChunkTicks<T> {

    public static ProtoChunkTicks<Block> BLOCKS = new NoopProtoChunkTick<>();
    public static ProtoChunkTicks<Fluid> FLUIDS = new NoopProtoChunkTick<>();

    @Override
    public @NotNull List<SavedTick<T>> scheduledTicks() {
        return List.of();
    }

    @Override
    public void schedule(final @NotNull ScheduledTick<T> orderedTick) {
    }

    @Override
    public boolean hasScheduledTick(final @NotNull BlockPos pos, final @NotNull T type) {
        return false;
    }

    @Override
    public int count() {
        return 0;
    }

    @Override
    public @NotNull Tag save(final long time, final @NotNull Function<T, String> typeToNameFunction) {
        return new ListTag();
    }

}
