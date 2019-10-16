package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * Soft reference implementation of {@link ReferenceChunk}
 */
public class SoftChunk extends ReferenceChunk {

    public SoftChunk(final IChunk parent, final IQueueExtent queueExtent) {
        super(parent, queueExtent);
    }

    @Override
    protected Reference<FinalizedChunk> toReference(final FinalizedChunk parent) {
        return new SoftReference<>(parent);
    }
}
