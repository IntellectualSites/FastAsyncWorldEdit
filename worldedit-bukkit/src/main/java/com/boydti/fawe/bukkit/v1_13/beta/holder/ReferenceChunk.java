package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

public abstract class ReferenceChunk implements IDelegateChunk {
    private final Reference<FinalizedChunk> ref;

    public ReferenceChunk(IChunk parent) {
        this.ref = toRef(new FinalizedChunk(parent));
    }

    protected abstract Reference<FinalizedChunk> toRef(FinalizedChunk parent);

    @Override
    public IChunk getParent() {
        FinalizedChunk finalized = ref.get();
        return finalized != null ? finalized.getParent() : null;
    }
}
