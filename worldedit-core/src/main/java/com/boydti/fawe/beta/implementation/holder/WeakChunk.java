package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class WeakChunk extends ReferenceChunk {
    public WeakChunk(final IChunk parent) {
        super(parent);
    }

    @Override
    protected Reference<FinalizedChunk> toRef(final FinalizedChunk parent) {
        return new WeakReference<>(parent);
    }
}
