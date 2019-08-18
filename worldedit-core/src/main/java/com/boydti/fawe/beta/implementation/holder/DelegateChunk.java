package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IDelegateChunk;
import com.sk89q.jnbt.CompoundTag;

import java.util.concurrent.Future;

/**
 * Implementation of IDelegateChunk
 */
public class DelegateChunk<U extends IChunk> implements IDelegateChunk<U> {

    private U parent;

    public DelegateChunk(U parent) {
        this.parent = parent;
    }

    @Override
    public final U getParent() {
        return parent;
    }

    public final void setParent(U parent) {
        this.parent = parent;
    }
}
