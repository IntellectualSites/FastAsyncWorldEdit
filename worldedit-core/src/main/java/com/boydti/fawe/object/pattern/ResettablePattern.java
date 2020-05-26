package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Resettable;

public interface ResettablePattern extends Resettable {

    @Override
    void reset();
}
