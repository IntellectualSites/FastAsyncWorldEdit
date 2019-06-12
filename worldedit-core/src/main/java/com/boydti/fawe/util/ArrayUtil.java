package com.boydti.fawe.util;

import java.util.Arrays;

public class ArrayUtil {
    public static final void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static final void fill(char[] a, int fromIndex, int toIndex, char val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
