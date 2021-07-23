package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;

public interface FilterBlockMask {

    boolean applyBlock(FilterBlock block);
}
