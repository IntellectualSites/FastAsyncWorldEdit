package com.fastasyncworldedit.core.command;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.util.formatting.text.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class SuggestInputParseException extends InputParseException {

    private final InputParseException cause;
    private final Supplier<List<String>> getSuggestions;

    /**
     * @deprecated Use {@link SuggestInputParseException#SuggestInputParseException(Component, Supplier)}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public SuggestInputParseException(String msg, String prefix, Supplier<List<String>> getSuggestions) {
        this(new InputParseException(msg), getSuggestions);
    }

    /**
     * @deprecated Use {@link SuggestInputParseException#of(Throwable, Supplier)}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public static SuggestInputParseException of(Throwable other, String prefix, Supplier<List<String>> getSuggestions) {
        if (other instanceof InputParseException) {
            return of((InputParseException) other, getSuggestions);
        }
        return of(new InputParseException(other.getMessage()), getSuggestions);
    }

    /**
     * @deprecated Use {@link SuggestInputParseException#of(InputParseException, Supplier)}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public static SuggestInputParseException of(InputParseException other, String prefix, Supplier<List<String>> getSuggestions) {
        if (other instanceof SuggestInputParseException) {
            return (SuggestInputParseException) other;
        }
        return new SuggestInputParseException(other, getSuggestions);
    }

    /**
     * @deprecated Use {@link SuggestInputParseException#SuggestInputParseException(InputParseException, Supplier)}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public SuggestInputParseException(InputParseException other, String prefix, Supplier<List<String>> getSuggestions) {
        super(other.getRichMessage());
        checkNotNull(getSuggestions);
        checkNotNull(other);
        this.cause = other;
        this.getSuggestions = getSuggestions;
    }

    /**
     * Create a new SuggestInputParseException instance
     *
     * @param message        Message to send
     * @param getSuggestions Supplier of list of suggestions to give to user
     * @since 2.6.2
     */
    public SuggestInputParseException(Component message, Supplier<List<String>> getSuggestions) {
        this(new InputParseException(message), getSuggestions);
    }

    public static SuggestInputParseException of(Throwable other, Supplier<List<String>> getSuggestions) {
        if (other instanceof InputParseException) {
            return of((InputParseException) other, getSuggestions);
        }
        //noinspection deprecation
        return of(new InputParseException(other.getMessage()), getSuggestions);
    }

    public static SuggestInputParseException of(InputParseException other, Supplier<List<String>> getSuggestions) {
        if (other instanceof SuggestInputParseException) {
            return (SuggestInputParseException) other;
        }
        return new SuggestInputParseException(other, getSuggestions);
    }

    public SuggestInputParseException(InputParseException other, Supplier<List<String>> getSuggestions) {
        super(other.getRichMessage());
        checkNotNull(getSuggestions);
        checkNotNull(other);
        this.cause = other;
        this.getSuggestions = getSuggestions;
    }

    public static SuggestInputParseException get(InvocationTargetException e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
            if (t instanceof SuggestInputParseException) {
                return (SuggestInputParseException) t;
            }
        }
        return null;
    }

    public static SuggestInputParseException of(String input, List<Object> values) {
        throw new SuggestInputParseException(Caption.of("fawe.error.no-value-for-input", input), () ->
                values.stream()
                        .map(Object::toString)
                        .filter(v -> v.startsWith(input))
                        .collect(Collectors.toList()));
    }

    @Override
    public synchronized Throwable getCause() {
        return cause.getCause();
    }

    @Override
    public String getMessage() {
        return cause.getMessage();
    }


    public List<String> getSuggestions() {
        return getSuggestions.get();
    }

    /**
     * @deprecated Unused
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public SuggestInputParseException prepend(String input) {
        // Do nothing
        return this;
    }

}
