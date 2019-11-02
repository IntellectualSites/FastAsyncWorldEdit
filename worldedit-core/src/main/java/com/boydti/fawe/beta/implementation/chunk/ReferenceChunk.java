package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;
import com.boydti.fawe.beta.IQueueExtent;
import java.lang.ref.Reference;

/**
 * An {@link IChunk} may be wrapped by a ReferenceChunk if there is low memory. This class stores a
 * reference for garbage collection purposes. If it cleaned by garbage collection, the {@link
 * FinalizedChunk} logic is run.
 */
public abstract class ReferenceChunk implements IDelegateChunk {

    private final Reference<FinalizedChunk> reference;

    public ReferenceChunk(IChunk parent, IQueueExtent queueExtent) {
        this.reference = toReference(new FinalizedChunk(parent, queueExtent));
    }

    protected abstract Reference<FinalizedChunk> toReference(FinalizedChunk parent);

    @Override
    public IChunk getParent() {
        final FinalizedChunk finalized = reference.get();
        return finalized != null ? finalized.getParent() : null;
    }
}
