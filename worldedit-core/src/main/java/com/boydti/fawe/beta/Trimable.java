package com.boydti.fawe.beta;

/**
 * Interface for objects that can be trimmed (memory related)<br> - Trimming will reduce its memory
 * footprint
 */
public interface Trimable {

    /**
     * Trims the object, reducing its memory footprint
     *
     * @param aggressive if trimming should be aggressive e.g., Not return early when the first
     *                   element cannot be trimmed
     * @return if this object is empty at the end of the trim, and can therefore be deleted
     */
    boolean trim(boolean aggressive);
}
