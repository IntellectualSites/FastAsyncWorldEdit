package com.fastasyncworldedit.core.beta.implementation.processors;

import com.fastasyncworldedit.core.beta.IBatchProcessor;
import com.fastasyncworldedit.core.beta.IChunk;
import com.fastasyncworldedit.core.beta.IChunkGet;
import com.fastasyncworldedit.core.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class EmptyBatchProcessor implements IBatchProcessor {
    private static final EmptyBatchProcessor instance = new EmptyBatchProcessor();

    public static EmptyBatchProcessor getInstance() {
        return instance;
    }

    @NotNull
    public Extent construct(@Nullable Extent child) {
        return child;
    }

    @NotNull
    public IChunkSet processSet(@Nullable IChunk chunk, @Nullable IChunkGet get, @Nullable IChunkSet set) {
        return set;
    }

    @Override
    @NotNull
    public Future<IChunkSet> postProcessSet(@Nullable IChunk chunk, @Nullable IChunkGet get, @Nullable IChunkSet set) {
        // Doesn't need to do anything
        return CompletableFuture.completedFuture(set);
    }

    @NotNull
    public IBatchProcessor join(@Nullable IBatchProcessor other) {
        return other;
    }

    @NotNull
    public IBatchProcessor joinPost(@Nullable IBatchProcessor other) {
        return other;
    }

    private EmptyBatchProcessor() {
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.ADDING_BLOCKS;
    }

}
