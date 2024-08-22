package com.fastasyncworldedit.core.queue.implementation.blocks;

/**
 * This interface represents the block states stored in a chunk section.
 * It provides methods for efficient bulk operations.
 */
public sealed interface DataArray permits CharDataArray, IntDataArray {

    /**
     * The amount of entries in a {@link DataArray}.
     */
    int CHUNK_SECTION_SIZE = 16 * 16 * 16;

    /**
     * Creates a new {@link DataArray} with all entries set to {@code 0}.
     *
     * @return an empty {@link DataArray}.
     */
    static DataArray createEmpty() {
        if (CharDataArray.CAN_USE_CHAR_ARRAY) {
            return new CharDataArray();
        }
        return new IntDataArray();
    }

    /**
     * @param value the value to set all entries to.
     * {@return a {@link DataArray} with all entries set to the given value}
     */
    static DataArray createFilled(int value) {
        final DataArray array = createEmpty();
        array.setAll(value);
        return array;
    }

    /**
     * @param other the {@link DataArray} to copy.
     * @return a copy of the given {@link DataArray}.
     */
    static DataArray createCopy(DataArray other) {
        final DataArray array = createEmpty();
        other.copyInto(array);
        return array;
    }

    /**
     * @param index the index to look up.
     * @return the value at the given index.
     */
    int getAt(int index);

    /**
     * Sets the value at the given index to the given value.
     *
     * @param index the index to set.
     * @param value the value to set at the index.
     * @throws IndexOutOfBoundsException if {@code index > } {@value CHUNK_SECTION_SIZE}
     *                                   or {@code index < 0}.
     */
    void setAt(int index, int value);

    /**
     * Sets all values in the given range to the given value.
     *
     * @param start the start of the range to set, inclusive.
     * @param end   the end of the range to set, exclusive
     * @param value the value to set all entries in the given range to.
     * @throws IndexOutOfBoundsException if {@code start > } {@value CHUNK_SECTION_SIZE}
     *                                   or {@code start < 0}
     *                                   or {@code end + 1 > } {@value CHUNK_SECTION_SIZE}
     *                                   or {@code end < 0}.
     */
    void setRange(int start, int end, int value);

    /**
     * Sets all entries to the given value.
     * This is equivalent to calling {@link #setRange(int, int, int) setRange(0, CHUNK_SECTION_SIZE, value) }
     *
     * @param value the value to set all entries to.
     */
    void setAll(int value);

    /**
     * Copies the data from this array into {@code other}.
     *
     * @param other the {@link DataArray} to copy the values from this array into.
     */
    void copyInto(DataArray other);

    /**
     * {@return {@code true} if all values are {@code 0}}
     */
    boolean isEmpty();

}
