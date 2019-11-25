package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Weak reference implementation of {@link ReferenceChunk}
 */
public class WeakChunk extends ReferenceChunk {

    public WeakChunk(IQueueChunk parent, IQueueExtent queueExtent) {
        super(parent, queueExtent);
    }

    @Override
    protected Reference<FinalizedChunk> toReference(final FinalizedChunk parent) {
        return new WeakReference<>(parent);
    }
}
