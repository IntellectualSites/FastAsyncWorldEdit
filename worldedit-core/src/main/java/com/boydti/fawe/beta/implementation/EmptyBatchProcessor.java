package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;

public enum EmptyBatchProcessor implements IBatchProcessor {
    INSTANCE
    ;
    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return set;
    }

    @Override
    public Extent construct(Extent child) {
        return child;
    }

    @Override
    public IBatchProcessor join(IBatchProcessor other) {
        return other;
    }
}
