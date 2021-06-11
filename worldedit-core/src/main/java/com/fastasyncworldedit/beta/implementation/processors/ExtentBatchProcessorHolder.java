package com.fastasyncworldedit.beta.implementation.processors;

import com.fastasyncworldedit.beta.IBatchProcessor;
import com.fastasyncworldedit.configuration.Settings;
import com.fastasyncworldedit.object.changeset.AbstractChangeSet;
import com.sk89q.worldedit.extent.Extent;

public abstract class ExtentBatchProcessorHolder extends BatchProcessorHolder implements Extent {
    @Override
    public Extent addProcessor(IBatchProcessor processor) {
        join(processor);
        return this;
    }

    @Override
    public Extent addPostProcessor(IBatchProcessor processor) {
        joinPost(processor);
        return this;
    }

    @Override
    public Extent enableHistory(AbstractChangeSet changeSet) {
        if (Settings.IMP.HISTORY.SEND_BEFORE_HISTORY) {
            return this.addPostProcessor(changeSet);
        } else {
            return this.addProcessor(changeSet);
        }
    }

    @Override
    public Extent disableHistory() {
        this.remove(AbstractChangeSet.class);
        return this;
    }
}
