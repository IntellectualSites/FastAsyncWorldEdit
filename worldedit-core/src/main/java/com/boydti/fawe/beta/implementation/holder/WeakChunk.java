package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Weak reference implementation of {@link ReferenceChunk}
 */
public class WeakChunk extends ReferenceChunk {
    public WeakChunk(final IChunk parent, final IQueueExtent queueExtent) {
        super(parent, queueExtent);
    }

    @Override
    protected Reference<FinalizedChunk> toRef(final FinalizedChunk parent) {
        return new WeakReference<>(parent);
    }
}
