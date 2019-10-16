package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;

/**
 * Implementation of IDelegateChunk
 * @param <T>
 */
public class DelegateChunk<T extends IChunk> implements IDelegateChunk {
    private T parent;

    public DelegateChunk(final T parent) {
        this.parent = parent;
    }

    public final T getParent() {
        return parent;
    }

    public final void setParent(final T parent) {
        this.parent = parent;
    }
}
