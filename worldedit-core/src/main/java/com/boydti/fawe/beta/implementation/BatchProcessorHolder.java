package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IBatchProcessor;

public class BatchProcessorHolder implements IBatchProcessorHolder {
    private IBatchProcessor processor = EmptyBatchProcessor.INSTANCE;

    @Override
    public IBatchProcessor getProcessor() {
        return processor;
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
