package com.fastasyncworldedit.core.object.mask;

import com.fastasyncworldedit.core.Resettable;

public interface ResettableMask extends Resettable {

    @Override
    void reset();
}
