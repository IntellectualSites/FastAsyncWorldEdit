package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;

public class FinalizedChunk extends DelegateChunk {
    public FinalizedChunk(IChunk parent) {
        super(parent);
    }

    @Override
    protected void finalize() throws Throwable {
        apply();
        setParent(null);
        super.finalize();
    }
}
