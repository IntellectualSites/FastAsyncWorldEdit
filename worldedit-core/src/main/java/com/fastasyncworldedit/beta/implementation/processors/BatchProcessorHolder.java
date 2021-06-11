package com.fastasyncworldedit.beta.implementation.processors;

import com.fastasyncworldedit.beta.IBatchProcessor;
import com.fastasyncworldedit.beta.IChunk;
import com.fastasyncworldedit.beta.IChunkGet;
import com.fastasyncworldedit.beta.IChunkSet;

import java.util.concurrent.Future;

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
    public Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
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

    @Override
    public ProcessorScope getScope() {
        return getProcessor().getScope();
    }
}
