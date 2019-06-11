package com.boydti.fawe.util;

public class CachedMathMan {
    private static final int ATAN2_BITS = 7;
    private static final int ATAN2_BITS2 = ATAN2_BITS << 1;
    private static final int ATAN2_MASK = ~(-1 << ATAN2_BITS2);
    private static final int ATAN2_COUNT = ATAN2_MASK + 1;


    private static float[] ANGLES = new float[65536];
    private static char[] SQRT = new char[65536];

    static {
        for (int i = 0; i < 65536; ++i) {
            ANGLES[i] = (float) Math.sin((double) i * 3.141592653589793D * 2.0D / 65536.0D);
        }
    }

    static {
        for (int i = 0; i < SQRT.length; i++) {
            SQRT[i] = (char) Math.round(Math.sqrt(i));
        }
    }

    /**
     * Optimized for i elem 0,65536 (characters)
     *
     * @param i
     * @return square root
     */
    protected static int usqrt(int i) {
        return SQRT[i];
    }

    protected static float sinInexact(double paramFloat) {
        return ANGLES[(int) (paramFloat * 10430.378F) & 0xFFFF];
    }

    protected static float cosInexact(double paramFloat) {
        return ANGLES[(int) (paramFloat * 10430.378F + 16384.0F) & 0xFFFF];
    }

}
