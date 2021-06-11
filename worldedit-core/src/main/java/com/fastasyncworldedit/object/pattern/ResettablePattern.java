package com.fastasyncworldedit.object.pattern;

import com.fastasyncworldedit.Resettable;

public interface ResettablePattern extends Resettable {

    @Override
    void reset();
}
