package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.Filter;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;

public interface VectorizedFilter extends Filter {

    /**
     * Applies a filter to a vector pair of get and set.
     *
     * @param get  the get vector
     * @param set  the set vector
     * @param mask the mask with the lanes set to true which should be affected by the filter
     * @return the resulting set vector.
     */
    ShortVector applyVector(ShortVector get, ShortVector set, VectorMask<Short> mask);

}
