package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;

public class BatchProcessorHolder implements IBatchProcessorHolder {
    private IBatchProcessor processor = EmptyBatchProcessor.INSTANCE;

    @Override
    public IBatchProcessor getProcessor() {
        return processor;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        System.out.println("Process set");
        System.out.println(getProcessor());
        return getProcessor().processSet(chunk, get, set);
    }

    @Override
    public void setProcessor(IBatchProcessor set) {
        this.processor = set;
    }

    @Override
    public String toString() {
        return super.toString() + "{" + getProcessor() + "}";
    }
}
