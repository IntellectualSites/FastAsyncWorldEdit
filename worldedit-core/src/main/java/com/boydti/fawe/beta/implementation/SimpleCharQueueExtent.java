package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.CharFilterBlock;
import com.boydti.fawe.beta.ChunkFilterBlock;

public abstract class SimpleCharQueueExtent extends SingleThreadQueueExtent {
    @Override
    public ChunkFilterBlock initFilterBlock() {
        return new CharFilterBlock(this);
    }
}
