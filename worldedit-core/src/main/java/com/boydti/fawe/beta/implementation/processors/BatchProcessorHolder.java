package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;

public class BatchProcessorHolder implements IBatchProcessorHolder {
    private IBatchProcessor processor = EmptyBatchProcessor.INSTANCE;

    @Override
    public IBatchProcessor getProcessor() {
        return processor;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return getProcessor().processSet(chunk, get, set);
    }

    @Override
    public void setProcessor(IBatchProcessor set) {
        this.processor = set;
    }

    @Override
    public String toString() {
        IBatchProcessor tmp = getProcessor();
        return super.toString() + "{" + (tmp == this ? "" : getProcessor()) + "}";
    }
}
