package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.extent.Extent;

public abstract class ExtentBatchProcessorHolder extends BatchProcessorHolder implements Extent {
    @Override
    public Extent addProcessor(IBatchProcessor processor) {
        join(processor);
        return this;
    }

    @Override
    public Extent enableHistory(FaweChangeSet changeSet) {
        return this.addProcessor(changeSet);
    }

    @Override
    public Extent disableHistory() {
        this.remove(FaweChangeSet.class);
        return this;
    }
}
