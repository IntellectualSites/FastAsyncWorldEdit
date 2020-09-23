package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;

public class BatchProcessorHolder implements IBatchProcessorHolder {
    private IBatchProcessor processor = EmptyBatchProcessor.getInstance();
    private IBatchProcessor postProcessor = EmptyBatchProcessor.getInstance();

    @Override
    public IBatchProcessor getProcessor() {
        return processor;
    }

    @Override
    public IBatchProcessor getPostProcessor() {
        return postProcessor;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return getProcessor().processSet(chunk, get, set);
    }

    @Override
    public IChunkSet postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return getPostProcessor().postProcessSet(chunk, get, set);
    }

    @Override
    public void flush() {
        getProcessor().flush();
    }

    @Override
    public void setProcessor(IBatchProcessor set) {
        this.processor = set;
    }

    @Override
    public void setPostProcessor(IBatchProcessor set) {
        this.postProcessor = set;
    }

    @Override
    public String toString() {
        IBatchProcessor tmp = getProcessor();
        return super.toString() + "{" + (tmp == this ? "" : getProcessor()) + "}";
    }
}
