package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.noop;

import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class NoopLightEngine extends ThreadedLevelLightEngine {

    private static final ProcessorMailbox<Runnable> MAILBOX = ProcessorMailbox.create(task -> {
    }, "fawe-no-op");
    private static final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> HANDLE = ProcessorHandle.of(
            "fawe-no-op",
            m -> {
            }
    );

    public NoopLightEngine(final LightChunkGetter lightChunkGetter) {
        //noinspection DataFlowIssue - nobody cares if the ChunkMap is null
        super(lightChunkGetter, null, false, MAILBOX, HANDLE);
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> lightChunk(final @NotNull ChunkAccess chunk, final boolean excludeBlocks) {
        return CompletableFuture.completedFuture(chunk);
    }

}
