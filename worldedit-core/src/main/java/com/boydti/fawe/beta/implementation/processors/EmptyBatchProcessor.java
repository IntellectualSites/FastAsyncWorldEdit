package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    public IChunkSet postProcessSet(@Nullable IChunk chunk, @Nullable IChunkGet get, @Nullable IChunkSet set) {
        return set;
    }

    @NotNull
    public IBatchProcessor join(@Nullable IBatchProcessor other) {
        return other;
    }

    private EmptyBatchProcessor() {
    }

}
