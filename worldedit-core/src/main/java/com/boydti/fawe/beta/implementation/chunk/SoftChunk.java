package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * Soft reference implementation of {@link ReferenceChunk}
 */
public class SoftChunk extends ReferenceChunk {

    public SoftChunk(IQueueChunk parent, IQueueExtent queueExtent) {
        super(parent, queueExtent);
    }

    @Override
    protected Reference<FinalizedChunk> toReference(final FinalizedChunk parent) {
        return new SoftReference<>(parent);
    }
}
