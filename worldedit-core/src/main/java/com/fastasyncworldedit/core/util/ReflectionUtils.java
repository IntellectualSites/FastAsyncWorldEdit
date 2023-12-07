package com.fastasyncworldedit.core.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This is an internal class not meant to be used outside the FAWE internals.
 */
public class ReflectionUtils {

    private static final VarHandle REFERENCE_ARRAY_HANDLE = MethodHandles.arrayElementVarHandle(Object[].class);
    private static Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static <T> T as(Class<T> t, Object o) {
        return t.isInstance(o) ? t.cast(o) : null;
    }

    /**
     * Performs CAS on the array element at the given index.
     *
     * @param array         the array in which to compare and set the value
     * @param expectedValue the value expected to be at the index
     * @param newValue      the new value to be set at the index if the expected value matches
     * @param index         the index at which to compare and set the value
     * @param <T>           the type of elements in the array
     * @return true if the value at the index was successfully updated to the new value, false otherwise
     * @see VarHandle#compareAndSet(Object...)
     */
    public static <T> boolean compareAndSet(T[] array, T expectedValue, T newValue, int index) {
        return REFERENCE_ARRAY_HANDLE.compareAndSet(array, index, expectedValue, newValue);
    }

    public static void setAccessibleNonFinal(Field field) {
        // let's make the field accessible
        field.setAccessible(true);

        // next we change the modifier in the Field instance to
        // not be final anymore, thus tricking reflection into
        // letting us modify the static final field
        if (Modifier.isFinal(field.getModifiers())) {
            try {
                Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                lookupField.setAccessible(true);

                // blank out the final bit in the modifiers int
                ((MethodHandles.Lookup) lookupField.get(null))
                        .findSetter(Field.class, "modifiers", int.class)
                        .invokeExact(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void setFailsafeFieldValue(Field field, Object target, Object value) throws IllegalAccessException {
        setAccessibleNonFinal(field);
        field.set(target, value);
    }

    public static Object getHandle(Object wrapper) {
        final Method getHandle = makeMethod(wrapper.getClass(), "getHandle");
        return callMethod(getHandle, wrapper);
    }

    //Utils
    public static Method makeMethod(Class<?> clazz, String methodName, Class<?>... parameters) {
        try {
            return clazz.getDeclaredMethod(methodName, parameters);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T callMethod(Method method, Object instance, Object... parameters) {
        if (method == null) {
            throw new RuntimeException("No such method");
        }
        method.setAccessible(true);
        try {
            return (T) method.invoke(instance, parameters);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends AccessibleObject> T setAccessible(T ao) {
        ao.setAccessible(true);
        return ao;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(@Nonnull Field field, Object instance) {
        field.setAccessible(true);
        try {
            return (T) field.get(instance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldThrowing(@Nonnull Field field, Object instance) throws IllegalAccessException {
        field.setAccessible(true);
        return (T) field.get(instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldOr(@Nonnull Field field, Object instance, @NonNull Supplier<T> fallback) {
        field.setAccessible(true);
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            return fallback.get();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldOr(@Nonnull Field field, Object instance, @NonNull Function<IllegalAccessException, T> fallback) {
        field.setAccessible(true);
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            return fallback.apply(e);
        }
    }

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static <T> Class<? extends T> getClass(String name, Class<T> superClass) {
        try {
            return Class.forName(name).asSubclass(superClass);
        } catch (ClassCastException | ClassNotFoundException ex) {
            return null;
        }
    }

    public static void unsafeSet(Field field, Object base, Object value) {
        UNSAFE.putObject(base, UNSAFE.objectFieldOffset(field), value);
    }

    /**
     * @return an instance of {@link Unsafe}
     */
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

}
