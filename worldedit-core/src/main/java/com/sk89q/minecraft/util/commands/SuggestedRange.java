package com.sk89q.minecraft.util.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SuggestedRange {
    /**
     * The minimum value that the number can be at, inclusive.
     *
     * @return the minimum value
     */
    double min() default Double.MIN_VALUE;

    /**
     * The maximum value that the number can be at, inclusive.
     *
     * @return the maximum value
     */
    double max() default Double.MAX_VALUE;
}
