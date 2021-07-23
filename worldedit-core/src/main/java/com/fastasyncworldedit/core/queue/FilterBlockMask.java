package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.queue.implementation.filter.block.FilterBlock;

public interface FilterBlockMask {

    boolean applyBlock(FilterBlock block);
}
