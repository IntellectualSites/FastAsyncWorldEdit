package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;

/**
 * Implementation of IDelegateChunk
 *
 * @param <T>
 */
public class DelegateChunk<T extends IChunk> implements IDelegateChunk {

    private T parent;

    public DelegateChunk(T parent) {
        this.parent = parent;
    }

    @Override
    public final T getParent() {
        return parent;
    }

    public final void setParent(T parent) {
        this.parent = parent;
    }
}
