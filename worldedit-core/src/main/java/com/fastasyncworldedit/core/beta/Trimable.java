package com.fastasyncworldedit.core.beta;

/**
 * Interface for objects that can be trimmed (memory related). Trimming will reduce its memory
 * footprint.
 */
public interface Trimable {

    /**
     * Trims the object, reducing its memory footprint.
     *
     * @param aggressive if trimming should be aggressive e.g., Not returning early when the first
     *                   element cannot be trimmed
     * @return if this object is empty at the end of the trim, and can therefore be deleted
     */
    boolean trim(boolean aggressive);

    default void recycle() {
    }
}
