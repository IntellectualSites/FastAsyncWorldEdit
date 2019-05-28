package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.CharFilterBlock;
import com.boydti.fawe.beta.ChunkFilterBlock;
import com.boydti.fawe.beta.FilterBlock;

public abstract class SimpleCharQueueExtent extends SingleThreadQueueExtent {
    @Override
    public ChunkFilterBlock initFilterBlock() {
        return new CharFilterBlock(this);
    }
}
