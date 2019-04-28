package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;
import com.boydti.fawe.beta.IQueueExtent;

import java.lang.ref.Reference;

public abstract class ReferenceChunk implements IDelegateChunk {
    private final Reference<FinalizedChunk> ref;

    public ReferenceChunk(final IChunk parent, IQueueExtent queueExtent) {
        this.ref = toRef(new FinalizedChunk(parent, queueExtent));
    }

    protected abstract Reference<FinalizedChunk> toRef(FinalizedChunk parent);

    @Override
    public IChunk getParent() {
        final FinalizedChunk finalized = ref.get();
        return finalized != null ? finalized.getParent() : null;
    }
}
