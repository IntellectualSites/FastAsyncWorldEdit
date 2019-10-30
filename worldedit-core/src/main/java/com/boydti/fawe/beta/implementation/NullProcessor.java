package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;

public enum NullProcessor implements IBatchProcessor {
    INSTANCE;
    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        System.out.println("Null process " + chunk.getX() + "," + chunk.getZ());
        return null;
    }

    @Override
    public Extent construct(Extent child) {
        return new NullExtent();
    }
}
