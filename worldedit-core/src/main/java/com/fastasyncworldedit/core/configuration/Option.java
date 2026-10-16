package com.fastasyncworldedit.core.configuration;

import java.io.Serializable;

/**
 * Names an option of the configuration by a reference to its accessor, e.g. {@code Settings.Queue::parallelThreads},
 * so that code changing an option is checked by the compiler and found by the IDE.
 * <p>
 * Only a method reference to an accessor of a section works, a lambda does not. The options of named groups, such as
 * the limit groups, cannot be named this way as an accessor does not say which group is meant.
 *
 * @param <S> the section the option is in
 * @since TODO
 */
public sealed interface Option<S extends Record> extends Serializable {

    /**
     * A {@code boolean} option.
     */
    @FunctionalInterface
    non-sealed interface Bool<S extends Record> extends Option<S> {

        boolean get(S section);

    }

    /**
     * An {@code int} option.
     */
    @FunctionalInterface
    non-sealed interface Int<S extends Record> extends Option<S> {

        int get(S section);

    }

    /**
     * A {@code String} option.
     */
    @FunctionalInterface
    non-sealed interface Text<S extends Record> extends Option<S> {

        String get(S section);

    }

    /**
     * A list option.
     */
    @FunctionalInterface
    non-sealed interface List<S extends Record> extends Option<S> {

        java.util.List<String> get(S section);

    }

    /**
     * An enum option.
     */
    @FunctionalInterface
    non-sealed interface Choice<S extends Record, E extends Enum<E>> extends Option<S> {

        E get(S section);

    }

}
