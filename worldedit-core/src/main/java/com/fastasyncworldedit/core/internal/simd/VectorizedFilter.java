package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.Filter;
import jdk.incubator.vector.ShortVector;

public interface VectorizedFilter extends Filter {
    ShortVector applyVector(ShortVector get, ShortVector set);
}
