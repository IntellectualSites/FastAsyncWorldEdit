package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.Resettable;

public interface ResettablePattern extends Resettable {

    @Override
    void reset();
}
