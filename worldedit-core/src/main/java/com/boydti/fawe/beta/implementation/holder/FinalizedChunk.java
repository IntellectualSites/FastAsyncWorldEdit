package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.IChunk;

/**
 * Used by {@link ReferenceChunk} to allow the chunk to be garbage collected
 *  - When the object is finalized, add it to the queue
 */
public class FinalizedChunk extends DelegateChunk {
    private final IQueueExtent queueExtent;

    public FinalizedChunk(final IChunk parent, IQueueExtent queueExtent) {
        super(parent);
        this.queueExtent = queueExtent;
    }

    /**
     * Submit the chunk to the queue
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        if (getParent() != null) {
            // TODO apply safely
//            apply();
            setParent(null);
        }
        super.finalize();
    }
}