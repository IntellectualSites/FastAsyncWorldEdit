package com.fastasyncworldedit.core.util;

public class CachedMathMan {

    private static final int ATAN2_BITS = 7;
    private static final int ATAN2_BITS2 = ATAN2_BITS << 1;
    private static final int ATAN2_MASK = ~(-1 << ATAN2_BITS2);
    private static final int ATAN2_COUNT = ATAN2_MASK + 1;
    private static final int ATAN2_DIM = (int) Math.sqrt(ATAN2_COUNT);
    private static final float INV_ATAN2_DIM_MINUS_1 = 1.0f / (ATAN2_DIM - 1);
    private static final float[] atan2 = new float[ATAN2_COUNT];

    static {
        for (int i = 0; i < ATAN2_DIM; i++) {
            for (int j = 0; j < ATAN2_DIM; j++) {
                float x0 = (float) i / ATAN2_DIM;
                float y0 = (float) j / ATAN2_DIM;

                atan2[(j * ATAN2_DIM) + i] = (float) Math.atan2(y0, x0);
            }
        }
    }

    private static final float[] ANGLES = new float[65536];

    static {
        for (int i = 0; i < 65536; ++i) {
            ANGLES[i] = (float) Math.sin((double) i * Math.PI * 2.0D / 65536.0D);
        }
    }

    private static final char[] SQRT = new char[65536];

    static {
        for (int i = 0; i < SQRT.length; i++) {
            SQRT[i] = (char) Math.round(Math.sqrt(i));
        }
    }

    /**
     * Optimized for i elem 0,65536 (characters).
     *
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

    protected static float atan2(float i, float j) {
        float add;
        float mul;

        if (j < 0.0f) {
            if (i < 0.0f) {
                j = -j;
                i = -i;

                mul = 1.0f;
            } else {
                j = -j;
                mul = -1.0f;
            }

            add = (float) -Math.PI;
        } else {
            if (i < 0.0f) {
                i = -i;
                mul = -1.0f;
            } else {
                mul = 1.0f;
            }

            add = 0.0f;
        }

        float invDiv = 1.0f / ((Math.max(j, i)) * INV_ATAN2_DIM_MINUS_1);

        int xi = (int) (j * invDiv);
        int yi = (int) (i * invDiv);

        return (atan2[(yi * ATAN2_DIM) + xi] + add) * mul;
    }

}
