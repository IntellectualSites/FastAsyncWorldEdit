package com.fastasyncworldedit.core.extension.factory.parser;

import com.fastasyncworldedit.core.configuration.Caption;
import com.google.common.base.Preconditions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A rich parser allows parsing of patterns and masks with extra arguments,
 * e.g. #simplex[scale][pattern].
 *
 * @param <E> the parse result.
 */
public abstract class RichParser<E> extends InputParser<E> implements AliasedParser {

    private final String[] prefixes;

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     * @param aliases   the prefix of this parser result.
     */
    protected RichParser(WorldEdit worldEdit, String... aliases) {
        super(worldEdit);
        Preconditions.checkArgument(aliases.length >= 1, "Aliases may not be empty");
        this.prefixes = aliases;
    }

    @Nonnull
    private static Predicate<String> validPrefix(String other) {
        return prefix -> {
            if (prefix.length() > other.length()) {
                return prefix.startsWith(other);
            }
            int i = other.indexOf('[');
            if (i == -1) {
                return other.equals(prefix);
            }
            return other.substring(0, i).equals(prefix);
        };
    }

    @Nonnull
    private Function<String, Stream<? extends String>> extractArguments(String input) {
        return prefix -> {
            if (input.length() > prefix.length() && input.startsWith(prefix + "[")) {
                // input already contains argument(s) -> extract them
                String[] strings = extractArguments(input.substring(prefix.length()), false);
                // rebuild the argument string without the last argument
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < strings.length - 1; i++) {
                    builder.append('[').append(strings[i]).append(']');
                }
                String previous = prefix + builder;
                // read the suggestions for the last argument
                return getSuggestions(strings[strings.length - 1], strings.length - 1)
                        .map(suggestion -> previous + "[" + suggestion);
            } else {
                return Stream.of(prefix);
            }
        };
    }

    /**
     * Gives the default prefix/name of the pattern/mask/transform.
     *
     * @return default prefix
     */
    public String getPrefix() {
        return this.prefixes[0];
    }

    /**
     * Return all prefix/name aliases of the pattern/mask/transform
     *
     * @return all prefix/name aliases
     */
    public List<String> getMatchedAliases() {
        return Arrays.asList(prefixes);
    }

    @Override
    public Stream<String> getSuggestions(String input) {
        return Arrays.stream(this.prefixes)
                .filter(validPrefix(input))
                .flatMap(extractArguments(input));
    }

    @Override
    public E parseFromInput(String input, ParserContext context) throws InputParseException {
        int i = input.indexOf('[');
        // Rich parser requires arguments, else, it should not be used
        if (i == -1) {
            return null;
        }
        String inputPrefix = input.substring(0, i);
        for (String prefix : this.prefixes) {
            if (!inputPrefix.equals(prefix)) {
                continue;
            }
            String[] arguments = extractArguments(input.substring(prefix.length()), true);
            return parseFromInput(arguments, context);
        }
        return null;
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
    protected abstract E parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException;

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
        if (!requireClosing) {
            if (open > 0) {
                arguments.add(input.substring(openIndex + 1));
            } else {
                int last = input.lastIndexOf(']');
                if (last != -1) {
                    arguments.add(input.substring(last));
                }
            }
        }
        if (requireClosing && open != 0) {
            throw new InputParseException(Caption.of("fawe.error.invalid-bracketing", TextComponent.of("'[' or ']'?")));
        }
        return arguments.toArray(new String[0]);
    }

}
