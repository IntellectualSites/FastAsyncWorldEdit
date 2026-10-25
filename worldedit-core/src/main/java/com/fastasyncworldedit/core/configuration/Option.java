package com.fastasyncworldedit.core.configuration;

import java.io.Serializable;

/**
 * An option of {@code config.yml}, given as a method reference to change it with {@link Settings#with} or
 * {@link Settings#override}.
 * <code>{@code
 * settings = settings.with(Settings.Queue::parallelThreads, 4);
 * }</code>
 * <p>
 * Unlike a path such as {@code queue.parallel-threads}, a method reference is checked by the compiler: the option must
 * exist and the value must have its type, so renaming or removing an option breaks the build instead of failing when
 * it runs.
 * <p>
 * Pass a method reference, not a lambda. The option is found from the name of the method, which a lambda does not
 * have, so passing a lambda throws an {@link IllegalArgumentException}. Options of a limit group, such as
 * {@code Settings.Limits::maxRadius}, cannot be changed this way, as the reference does not say which group is meant.
 *
 * @since 3.0.0
 */
public sealed interface Option extends Serializable {

    /**
     * A {@code boolean} option.
     *
     * @param <S> the record holding the option
     */
    @FunctionalInterface
    non-sealed interface Bool<S extends Record> extends Option {

        boolean get(S section);

    }

    /**
     * An {@code int} option.
     *
     * @param <S> the record holding the option
     */
    @FunctionalInterface
    non-sealed interface Int<S extends Record> extends Option {

        int get(S section);

    }

    /**
     * A {@code String} option.
     *
     * @param <S> the record holding the option
     */
    @FunctionalInterface
    non-sealed interface Text<S extends Record> extends Option {

        String get(S section);

    }

    /**
     * A list option.
     *
     * @param <S> the record holding the option
     */
    @FunctionalInterface
    non-sealed interface List<S extends Record> extends Option {

        java.util.List<String> get(S section);

    }

    /**
     * An enum option.
     *
     * @param <S> the record holding the option
     * @param <E> the enum of the option's values
     */
    @FunctionalInterface
    non-sealed interface Choice<S extends Record, E extends Enum<E>> extends Option {

        E get(S section);

    }

}
