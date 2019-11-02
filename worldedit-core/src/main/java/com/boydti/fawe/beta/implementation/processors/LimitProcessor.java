package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.extent.Extent;

public class LimitProcessor implements IBatchProcessor {
    private final FaweLimit limit;
    private final IBatchProcessor parent;
    public LimitProcessor(FaweLimit limit, IBatchProcessor parent) {
        this.limit = limit;
        this.parent = parent;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        try {
            return parent.processSet(chunk, get, set);
        } catch (FaweException e) {
            if (!limit.MAX_CHANGES()) {
                throw e;
            }
            return null;
        }
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        try {
            return parent.processGet(chunkX, chunkZ);
        } catch (FaweException e) {
            if (!limit.MAX_CHECKS()) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public Extent construct(Extent child) {
        return new LimitExtent(parent.construct(child), limit);
    }
}
