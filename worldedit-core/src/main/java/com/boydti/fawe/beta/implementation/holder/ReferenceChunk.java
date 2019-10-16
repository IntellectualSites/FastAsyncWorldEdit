package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;
import com.boydti.fawe.beta.IQueueExtent;
import java.lang.ref.Reference;

/**
 * An IChunk may be wrapped by a ReferenceChunk if there is low memory<br>
 * A reference chunk stores a reference (for garbage collection purposes)<br>
 *  - If it is garbage collected, the {@link FinalizedChunk} logic is run
 */
public abstract class ReferenceChunk implements IDelegateChunk {

    private final Reference<FinalizedChunk> reference;

    public ReferenceChunk(final IChunk parent, final IQueueExtent queueExtent) {
        this.reference = toReference(new FinalizedChunk(parent, queueExtent));
    }

    protected abstract Reference<FinalizedChunk> toReference(FinalizedChunk parent);

    @Override
    public IChunk getParent() {
        final FinalizedChunk finalized = reference.get();
        return finalized != null ? finalized.getParent() : null;
    }
}
