package com.fastasyncworldedit.core.extent.processor;

import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Future;

public final class NullProcessor implements IBatchProcessor {

    private static final NullProcessor instance = new NullProcessor();

    public static NullProcessor getInstance() {
        return instance;
    }

    @Nullable
    public IChunkSet processSet(@Nonnull IChunk chunk, @Nonnull IChunkGet get, @Nonnull IChunkSet set) {
        return null;
    }

    @Nonnull
    public Extent construct(@Nonnull Extent child) {
        return new NullExtent();
    }

    private NullProcessor() {
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.ADDING_BLOCKS;
    }

}
