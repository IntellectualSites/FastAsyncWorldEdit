package com.fastasyncworldedit.core.queue.implementation.processors;

import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public final class NullProcessor implements IBatchProcessor {
    private static final NullProcessor instance = new NullProcessor();

    public static NullProcessor getInstance() {
        return instance;
    }

    @Nullable
    public IChunkSet processSet(@NotNull IChunk chunk, @NotNull IChunkGet get, @NotNull IChunkSet set) {
        return null;
    }

    @Nullable
    public Future<IChunkSet> postProcessSet(@NotNull IChunk chunk, @NotNull IChunkGet get, @NotNull IChunkSet set) {
        return null;
    }

    @NotNull
    public Extent construct(@NotNull Extent child) {
        return new NullExtent();
    }

    private NullProcessor() {
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.ADDING_BLOCKS;
    }

}
