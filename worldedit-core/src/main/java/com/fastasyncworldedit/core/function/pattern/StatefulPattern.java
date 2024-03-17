package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.function.pattern.Pattern;

/**
 * An interface for any pattern that has non-thread-safe state
 * @since TODO
 */
public interface StatefulPattern extends Pattern {

    @Override
    default StatefulPattern fork() {
        throw new IllegalStateException(getClass() + " needs to implement fork()");
    }

}
