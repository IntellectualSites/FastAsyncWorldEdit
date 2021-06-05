package com.boydti.fawe.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * This is an internal class not meant to be used outside of the FAWE internals.
 */
public class UnsafeUtility {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError("Cannot access Unsafe");
        }
    }

    public static Unsafe getUNSAFE() {
        return UNSAFE;
    }
}
