package com.sk89q.worldedit.extension.factory.parser;

import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * A rich parser allows parsing of patterns and masks with extra arguments,
 * e.g. #simplex[scale][pattern].
 *
 * @param <E> the parse result.
 */
public abstract class RichParser<E> extends InputParser<E> {
    private final String prefix;
    private final String required;

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     * @param prefix    the prefix of this parser result.
     */
    protected RichParser(WorldEdit worldEdit, String prefix) {
        super(worldEdit);
        this.prefix = prefix;
        this.required = prefix + "[";
    }

    @Override
    public Stream<String> getSuggestions(String input) {
        // we don't even want to start suggesting if it's not meant to be this parser result
        if (input.length() > this.required.length() && !input.startsWith(this.required)) {
            return Stream.empty();
        }
        // suggest until the first [ as long as it isn't fully typed
        if (input.length() < this.required.length()) {
            return Stream.of(this.required).filter(s -> s.startsWith(input));
        }
        // we know that it is at least "<required>"
        String[] strings = extractArguments(input.substring(this.prefix.length()), false);
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < strings.length - 1; i++) {
            joiner.add("[" + strings[i] + "]");
        }
        String previous = this.prefix + joiner;
        return getSuggestions(strings[strings.length - 1], strings.length - 1).map(s -> previous + "[" + s + "]");
    }

    @Override
    public E parseFromInput(String input, ParserContext context) throws InputParseException {
        if (!input.startsWith(this.prefix)) return null;
        if (input.length() < this.prefix.length()) return null;
        String[] arguments = extractArguments(input.substring(prefix.length()), true);
        return parseFromInput(arguments, context);
    }

    /**
     * Returns a stream of suggestions for the argument at the given index.
     *
     * @param argumentInput the already provided input for the argument at the given index.
     * @param index         the index of the argument to get suggestions for.
     * @return a stream of suggestions matching the given input for the argument at the given index.
     */
    protected abstract Stream<String> getSuggestions(String argumentInput, int index);

    /**
     * Parses the already split arguments.
     *
     * @param arguments the array of arguments that were split (can be empty).
     * @param context   the context of this parsing process.
     * @return the resulting parsed type.
     * @throws InputParseException if the input couldn't be parsed correctly.
     */
    protected abstract E parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException;

    /**
     * Extracts arguments enclosed by {@code []} into an array.
     * Example: {@code [Hello][World]} results in a list containing {@code Hello} and {@code World}.
     *
     * @param input          the input to extract arguments from.
     * @param requireClosing whether or not the extraction requires valid bracketing.
     * @return an array of extracted arguments.
     * @throws InputParseException if {@code requireClosing == true} and the count of [ != the count of ]
     */
    protected String[] extractArguments(String input, boolean requireClosing) throws InputParseException {
        int open = 0; // the "level"
        int openIndex = 0;
        int i = 0;
        List<String> arguments = new ArrayList<>();
        for (; i < input.length(); i++) {
            if (input.charAt(i) == '[') {
                if (open++ == 0) {
                    openIndex = i;
                }
            }
            if (input.charAt(i) == ']') {
                if (--open == 0) {
                    arguments.add(input.substring(openIndex + 1, i));
                }
            }
        }
        if (!requireClosing && open > 0) {
            arguments.add(input.substring(openIndex + 1));
        }
        if (requireClosing && open != 0) {
            throw new InputParseException("Invalid bracketing, are you missing a '[' or ']'?");
        }
        return arguments.toArray(new String[0]);
    }
}
