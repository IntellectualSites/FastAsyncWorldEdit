package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.CharFilterBlock;
import com.boydti.fawe.beta.FilterBlock;

public abstract class SimpleCharQueueExtent extends SingleThreadQueueExtent {
    @Override
    public FilterBlock initFilterBlock() {
        CharFilterBlock filter = new CharFilterBlock();
        filter.init(this);
        return filter;
    }
}
