package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
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

}
