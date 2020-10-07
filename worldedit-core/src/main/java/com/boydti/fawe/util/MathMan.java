package com.boydti.fawe.util;

public class MathMan {

    /**
     * An optimized version of {@link Math#sqrt(double)} that accepts integers.
     * @param a a value
     * @return the positive square root of {@code a}.
     *         If the argument is NaN or less than zero, the result is NaN.
     */
    public static int usqrt(int a) {
        if (a < 65536) {
            return CachedMathMan.usqrt(a);
        }
        return (int) Math.round(Math.sqrt(a));
    }

    public static float sinInexact(double paramFloat) {
        return CachedMathMan.sinInexact(paramFloat);
    }

    public static float cosInexact(double paramFloat) {
        return CachedMathMan.cosInexact(paramFloat);
    }

    public static int log2nlz( int bits ) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(bits);
    }

    public static int floorZero(double d0) {
        int i = (int) d0;
        return d0 < (double) i ? i - 1 : i;
    }

    public static double max(double... values) {
        double max = Double.MIN_VALUE;
        for (double d : values) {
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    public static int max(int... values) {
        int max = Integer.MIN_VALUE;
        for (int d : values) {
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    public static int min(int... values) {
        return Math.min(Math.min(values[0], values[1]), values[2]);
    }

    public static double min(double... values) {
        return Math.min(Math.min(values[0], values[1]), values[2]);
    }

    public static int ceilZero(float floatNumber) {
        int floor = (int) floatNumber;
        return floatNumber > (float) floor ? floor + 1 : floor;
    }

    public static int sqr(int val) {
        return val * val;
    }

    public static int clamp(int check, int min, int max) {
        return check > max ? max : (Math.max(check, min));
    }

    public static float clamp(float check, float min, float max) {
        return check > max ? max : (Math.max(check, min));
    }

    public static double hypot(final double... pars) {
        double sum = 0;
        for (final double d : pars) {
            sum += Math.pow(d, 2);
        }
        return Math.sqrt(sum);
    }

    public static double hypot2(final double... pars) {
        double sum = 0;
        for (final double d : pars) {
            sum += Math.pow(d, 2);
        }
        return sum;
    }

    public static int wrap(int value, int min, int max) {
        if (max < min) {
            return value;
        }
        if (min == max) {
            return min;
        }
        int diff = max - min + 1;
        if (value < min) {
            return max - ((min - value) % diff);
        } else if (value > max) {
            return min + ((value - min) % diff);
        } else {
            return value;
        }
    }

    public static long inverseRound(double val) {
        long round = Math.round(val);
        return (long) (round + Math.signum(val - round));
    }

    public static int pair(short x, short y) {
        return (x << 16) | (y & 0xFFFF);
    }

    public static short unpairX(int hash) {
        return (short) (hash >> 16);
    }

    public static short unpairY(int hash) {
        return (short) (hash & 0xFFFF);
    }

    public static short pairByte(int a, int b) {
        return (short) ((a << 8) | (b & 0xFF));
    }

    public static byte unpairShortX(short pair) {
        return (byte) (pair >> 8);
    }

    public static byte unpairShortY(short pair) {
        return (byte) pair;
    }

    /**
     * Used to convert a set of coordinates into a packed long.
     * @param a typically, represents the x coordinate
     * @param b typically, represents the y coordinate
     * @return the packed coordinates
     */
    public static long pairInt(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    public static long tripleWorldCoord(int x, int y, int z) {
        return y + (((long) x & 0x3FFFFFF) << 8) + (((long) z & 0x3FFFFFF) << 34);
    }

    public static long untripleWorldCoordX(long triple) {
        return (((triple >> 8) & 0x3FFFFFF) << 38) >> 38;
    }

    public static long untripleWorldCoordY(long triple) {
        return triple & 0xFF;
    }

    public static long untripleWorldCoordZ(long triple) {
        return (((triple >> 34) & 0x3FFFFFF) << 38) >> 38;
    }

    public static short tripleBlockCoord(int x, int y, int z) {
        return (short) ((x & 15) << 12 | (z & 15) << 8 | y);
    }

    public static char tripleBlockCoordChar(int x, int y, int z) {
        return (char) ((x & 15) << 12 | (z & 15) << 8 | y);
    }

    public static int untripleBlockCoordX(int triple) {
        return (triple >> 12) & 0xF;
    }

    public static int untripleBlockCoordY(int triple) {
        return (triple & 0xFF);
    }

    public static int untripleBlockCoordZ(int triple) {
        return (triple >> 8) & 0xF;
    }

    public static int tripleSearchCoords(int x, int y, int z) {
        byte b1 = (byte) y;
        byte b3 = (byte) (x);
        byte b4 = (byte) (z);
        int x16 = (x >> 8) & 0x7;
        int z16 = (z >> 8) & 0x7;
        byte b2 = MathMan.pair8(x16, z16);
        return ((b1 & 0xFF)
                + ((b2 & 0x7F) << 8)
                + ((b3 & 0xFF) << 15)
                + ((b4 & 0xFF) << 23));
    }

    public static int pairSearchCoords(int x, int y) {
        byte b1 = (byte) ((x & 0xF) + ((y & 0xF) << 4));
        byte b2 = (byte) ((x >> 4) & 0xFF);
        byte b3 = (byte) ((y >> 4) & 0xFF);
        int x16 = (x >> 12) & 0xF;
        int y16 = (y >> 12) & 0xF;
        byte b4 = (byte) ((x16 & 0xF) + ((y16 & 0xF) << 4));
        return ((b1 & 0xFF)
                + ((b2 & 0xFF) << 8)
                + ((b3 & 0xFF) << 16)
                + ((b4 & 0xFF) << 24));
    }

    public static int unpairSearchCoordsX(int pair) {
        int x1 = (pair >> 24) & 0x7;
        int x2 = (pair >> 8) & 0xFF;
        int x3 = (pair & 0xF);
        return x3 + (x2 << 4) + (x1 << 12);
    }

    public static int unpairSearchCoordsY(int pair) {
        int y1 = ((pair >> 24) & 0x7F) >> 3;
        int y2 = (pair >> 16) & 0xFF;
        int y3 = (pair & 0xFF) >> 4;
        return y3 + (y2 << 4) + (y1 << 12);
    }

    public static long chunkXZ2Int(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public static int unpairIntX(long pair) {
        return (int) (pair >> 32);
    }

    public static int unpairIntY(long pair) {
        return (int) pair;
    }

    public static byte pair16(int x, int y) {
        return (byte) (x + (y << 4));
    }

    public static byte unpair16x(byte value) {
        return (byte) (value & 0xF);
    }

    public static byte unpair16y(byte value) {
        return (byte) ((value >> 4) & 0xF);
    }

    public static byte pair8(int x, int y) {
        return (byte) (x + (y << 3));
    }

    public static byte unpair8x(int value) {
        return (byte) (value & 0x7);
    }

    public static byte unpair8y(int value) {
        return (byte) ((value >> 3) & 0x7F);
    }

    public static int lossyFastDivide(int a, int b) {
        return (a * ((1 << 16) / b)) >> 16;
    }

    public static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    public static int gcd(int[] a) {
        int result = a[0];
        for (int i = 1; i < a.length; i++) {
            result = gcd(result, a[i]);
        }
        return result;
    }


    public static double getMean(int[] array) {
        double count = 0;
        for (int i : array) {
            count += i;
        }
        return count / array.length;
    }

    public static double getMean(double[] array) {
        double count = 0;
        for (double i : array) {
            count += i;
        }
        return count / array.length;
    }

    /**
     * Returns [x, y, z].
     */
    public static float[] getDirection(float yaw, float pitch) {
        double pitch_sin = Math.sin(pitch);
        return new float[]{(float) (pitch_sin * Math.cos(yaw)), (float) (pitch_sin * Math.sin(yaw)), (float) Math.cos(pitch)};
    }

    public static int roundInt(double value) {
        return (int) (value < 0 ? (value == (int) value) ? value : value - 1 : value);
    }

    /**
     * Returns [ pitch, yaw ].
     */
    public static float[] getPitchAndYaw(float x, float y, float z) {
        float distance = sqrtApprox((z * z) + (x * x));
        return new float[]{atan2(y, distance), atan2(x, z)};
    }

    public static float atan2(float i, float j) {
        return CachedMathMan.atan2(i, j);
    }

    public static float sqrtApprox(float f) {
        return f * Float.intBitsToFloat(0x5f375a86 - (Float.floatToIntBits(f) >> 1));
    }

    public static double sqrtApprox(double d) {
        return Double.longBitsToDouble(((Double.doubleToLongBits(d) - (1L << 52)) >> 1) + (1L << 61));
    }

    public static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - (xhalf * x * x));
        return x;
    }

    public static boolean isInteger(CharSequence str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if ((c <= '/') || (c >= ':')) {
                return false;
            }
        }
        return true;
    }

    public static double getSD(double[] array, double av) {
        double sd = 0;
        for (double element : array) {
            sd += Math.pow(Math.abs(element - av), 2);
        }
        return Math.sqrt(sd / array.length);
    }

    public static double getSD(int[] array, double av) {
        double sd = 0;
        for (int element : array) {
            sd += Math.pow(Math.abs(element - av), 2);
        }
        return Math.sqrt(sd / array.length);
    }

    public static int absByte(int value) {
        return (value ^ (value >> 8)) - (value >> 8);
    }

    public static int mod(int a, int b) {
        if (isPowerOfTwo(b)) {
            return a & (b - 1);
        }
        return a % b;
    }

    public static int unsignedMod(int a, int b) {
        if (isPowerOfTwo(b)) {
            return a & (b - 1);
        }
        return a % b;
    }

    public static boolean isPowerOfTwo(int a) {
        return (a & a - 1) == 0;
    }
}
