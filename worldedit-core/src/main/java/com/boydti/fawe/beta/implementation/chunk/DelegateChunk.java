package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;

/**
 * Implementation of IDelegateChunk
 */
public class DelegateChunk<T extends IChunk> implements IDelegateChunk<T> {

    private T parent;

    public DelegateChunk(final T parent) {
        this.parent = parent;
    }

    @Override
    public final T getParent() {
        return parent;
    }

    public final void setParent(final T parent) {
        this.parent = parent;
    }
}
