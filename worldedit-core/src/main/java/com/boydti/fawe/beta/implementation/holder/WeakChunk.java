package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * A {@link ReferenceChunk} using {@link WeakReference} to hold the chunk.
 */
public class WeakChunk extends ReferenceChunk {

    public WeakChunk(IChunk parent, IQueueExtent queueExtent) {
        super(parent, queueExtent);
    }

    @Override
    protected Reference<FinalizedChunk> toReference(FinalizedChunk parent) {
        return new WeakReference<>(parent);
    }
}
