package com.boydti.fawe.bukkit.chat;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.lang.Validate;

/**
 * Represents a wrapper around an array class of an arbitrary reference type,
 * which properly implements "value" hash code and equality functions.
 * <p>
 * This class is intended for use as a key to a map.
 * </p>
 *
 * @param <E> The type of elements in the array.
 * @author Glen Husman
 * @see Arrays
 */
public final class ArrayWrapper<E> {

    /**
     * Creates an array wrapper with some elements.
     *
     * @param elements The elements of the array.
     */
    @SafeVarargs
    public ArrayWrapper(E... elements) {
        setArray(elements);
    }

    private E[] _array;

    /**
     * Retrieves a reference to the wrapped array instance.
     *
     * @return The array wrapped by this instance.
     */
    public E[] getArray() {
        return _array;
    }

    /**
     * Set this wrapper to wrap a new array instance.
     *
     * @param array The new wrapped array.
     */
    public void setArray(E[] array) {
        Validate.notNull(array, "The array must not be null.");
        _array = array;
    }

    /**
     * Determines if this object has a value equivalent to another object.
     *
     * @see Arrays#equals(Object[], Object[])
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ArrayWrapper)) {
            return false;
        }
        return Arrays.equals(_array, ((ArrayWrapper) other)._array);
    }

    /**
     * Gets the hash code represented by this objects value.
     *
     * @return This object's hash code.
     * @see Arrays#hashCode(Object[])
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(_array);
    }

    /**
     * Converts an iterable element collection to an array of elements.
     * The iteration order of the specified object will be used as the array element order.
     *
     * @param list The iterable of objects which will be converted to an array.
     * @param c    The type of the elements of the array.
     * @return An array of elements in the specified iterable.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Iterable<? extends T> list, Class<T> c) {
        int size = -1;
        if (list instanceof Collection<?>) {
            @SuppressWarnings("rawtypes")
            Collection coll = (Collection) list;
            size = coll.size();
        }


        if (size < 0) {
            size = 0;
            // Ugly hack: Count it ourselves
            for (@SuppressWarnings("unused") T element : list) {
                size++;
            }
        }

        T[] result = (T[]) Array.newInstance(c, size);
        int i = 0;
        for (T element : list) { // Assumes iteration order is consistent
            result[i++] = element; // Assign array element at index THEN increment counter
        }
        return result;
    }

}
