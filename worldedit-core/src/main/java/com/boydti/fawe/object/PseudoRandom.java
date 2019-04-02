package com.boydti.fawe.object;

import java.util.concurrent.ThreadLocalRandom;

@Deprecated
/**
 * @Deprecated use ThreadLocalRandom instead
 */
public class PseudoRandom {
    public static PseudoRandom random = new PseudoRandom();

    public PseudoRandom() {
    }

    public int random(final int n) {
        if (n == 1) {
            return 0;
        }
        final long r = ((ThreadLocalRandom.current().nextLong() >>> 32) * n) >> 32;
        return (int) r;
    }

}
