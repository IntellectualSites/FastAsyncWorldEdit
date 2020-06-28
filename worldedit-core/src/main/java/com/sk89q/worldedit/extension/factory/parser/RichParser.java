package com.sk89q.worldedit.extension.factory.parser;

import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public abstract class RichParser<E> extends InputParser<E> {
    private final String prefix;
    private final String required;

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
        // if it's not "atomic", we can't parse it like that
        if (StringUtil.split(input, ',', '[', ']').size() > 1) return null;
        // TODO if no arguments -> exception
        // TODO if not starts with [ and ends with ] -> exception
        String[] arguments = extractArguments(input.substring(prefix.length()), true);
        return parseFromInput(arguments, context);
    }

    protected abstract Stream<String> getSuggestions(String argument, int index);

    protected abstract E parseFromInput(String[] arguments, ParserContext context);

    /**
     * Extracts arguments enclosed by {@code []} into an array.
     * Example: {@code [Hello][World]} results in a list containing {@code Hello} and {@code World}.
     *
     * @param input          the input to extract arguments from.
     * @param requireClosing whether or not the extraction requires valid bracketing.
     * @return an array of extracted arguments.
     */
    protected String[] extractArguments(String input, boolean requireClosing) {
        int open = 0;
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
        // TODO if open != 0 -> exception
        return arguments.toArray(new String[0]);
    }
}
