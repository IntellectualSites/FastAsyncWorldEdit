package com.fastasyncworldedit.core.object.pattern;

import com.fastasyncworldedit.core.Resettable;

public interface ResettablePattern extends Resettable {

    @Override
    void reset();
}
