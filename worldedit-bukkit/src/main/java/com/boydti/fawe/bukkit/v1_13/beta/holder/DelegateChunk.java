package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;

public class DelegateChunk<T extends IChunk> implements IDelegateChunk {
    private T parent;

    public DelegateChunk(T parent) {
        this.parent = parent;
    }

    public final T getParent() {
        return parent;
    }

    public final void setParent(T parent) {
        this.parent = parent;
    }
}
