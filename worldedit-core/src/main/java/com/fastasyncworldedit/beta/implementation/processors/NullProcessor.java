package com.fastasyncworldedit.beta.implementation.processors;

import com.fastasyncworldedit.beta.IBatchProcessor;
import com.fastasyncworldedit.beta.IChunk;
import com.fastasyncworldedit.beta.IChunkGet;
import com.fastasyncworldedit.beta.IChunkSet;
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
