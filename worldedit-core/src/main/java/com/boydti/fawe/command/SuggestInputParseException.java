package com.boydti.fawe.command;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.extension.input.InputParseException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SuggestInputParseException extends InputParseException {

    private final InputParseException cause;
    private final Supplier<List<String>> getSuggestions;
    private String prefix;

    public SuggestInputParseException(String msg, String prefix, Supplier<List<String>> getSuggestions) {
        this(new InputParseException(msg), prefix, getSuggestions);
    }

    public static SuggestInputParseException of(Throwable other, String prefix, Supplier<List<String>> getSuggestions) {
        if (other instanceof InputParseException) return of((InputParseException) other, prefix, getSuggestions);
        return of(new InputParseException(other.getMessage()), prefix, getSuggestions);
    }

    public static SuggestInputParseException of(InputParseException other, String prefix, Supplier<List<String>> getSuggestions) {
        if (other instanceof SuggestInputParseException) return (SuggestInputParseException) other;
        return new SuggestInputParseException(other, prefix, getSuggestions);
    }

    public SuggestInputParseException(InputParseException other, String prefix, Supplier<List<String>> getSuggestions) {
        super(other.getMessage());
        checkNotNull(getSuggestions);
        checkNotNull(other);
        this.cause = other;
        this.getSuggestions = getSuggestions;
        this.prefix = prefix;
    }

    public static SuggestInputParseException get(InvocationTargetException e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
            if (t instanceof SuggestInputParseException) return (SuggestInputParseException) t;
        }
        return null;
    }

    public static SuggestInputParseException of(String input, List<Object> values) {
        throw new SuggestInputParseException("No value: " + input, input, () ->
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

    public SuggestInputParseException prepend(String input) {
        this.prefix = input + prefix;
        return this;
    }
}
