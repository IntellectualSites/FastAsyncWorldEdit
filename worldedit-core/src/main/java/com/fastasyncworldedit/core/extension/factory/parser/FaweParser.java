package com.fastasyncworldedit.core.extension.factory.parser;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.internal.registry.InputParser;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class FaweParser<T> extends InputParser<T> implements AliasedParser {

    protected FaweParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    /**
     * Parse an input into a list of {@link java.util.Map.Entry} of {@link ParseEntry} and a list of the given arguments, where
     * arguments are given in square brackets, e.g. {@code #offset[2][10][2]}. Different entries may be separated by , or &amp;
     * (OR and AND respectively)
     *
     * @param toParse the string to parse
     * @return a list of parsed entries and their arguments
     * @throws InputParseException if the input is not complete (has dangling characters)
     */
    public static List<Map.Entry<ParseEntry, List<String>>> parse(String toParse) throws InputParseException {
        List<Map.Entry<ParseEntry, List<String>>> keys = new ArrayList<>();
        List<String> inputs = new ArrayList<>();
        List<Boolean> and = new ArrayList<>();
        int last = 0;
        boolean expression = false;
        for (int i = 0; i < toParse.length(); i++) {
            char c = toParse.charAt(i);
            switch (c) {
                case ',', '&', ' ' -> {
                    if (expression) {
                        continue;
                    }
                    String result = toParse.substring(last, i);
                    if (!result.isEmpty()) {
                        inputs.add(result);
                        and.add(c == '&' || c == ' ');
                    } else {
                        throw new InputParseException(Caption.of("fawe.error.parse.invalid-dangling-character", c));
                    }
                    last = i + 1;
                }
                case '=' -> expression = true;
                default -> {
                    if (c == '[') {
                        int next = StringMan.findMatchingBracket(toParse, i);
                        if (next != -1) {
                            i = next;
                        } else {
                            toParse += "]";
                            i = toParse.length();
                        }
                        expression = false;
                    }
                }
            }
        }
        inputs.add(toParse.substring(last));
        for (int i = 0; i < inputs.size(); i++) {
            String full = inputs.get(i);
            String command = full;
            List<String> args = new ArrayList<>();
            while (!command.isEmpty() && command.charAt(command.length() - 1) == ']') {
                int startPos = StringMan.findMatchingBracket(command, command.length() - 1);
                if (startPos == -1) {
                    break;
                }
                String arg = command.substring(startPos + 1, command.length() - 1);
                args.add(arg);
                command = full.substring(0, startPos);
            }
            Collections.reverse(args);
            ParseEntry entry = new ParseEntry(full, command, i > 0 ? and.get(i - 1) : false);
            keys.add(new AbstractMap.SimpleEntry<>(entry, args));
        }
        return keys;
    }

    protected PlatformCommandManager getPlatform() {
        return PlatformCommandManager.getInstance();
    }

    public static class ParseEntry {

        private final boolean and;
        private final String input;
        private final String full;

        public ParseEntry(String full, String input, boolean type) {
            this.full = full;
            this.input = input;
            this.and = type;
        }

        /**
         * Gives if the parsed entry was appended to the original input as an AND.
         *
         * @return if appended to input with '&amp;' rather than ','
         */
        public boolean isAnd() {
            return and;
        }

        /**
         * The input "name" e.g. for {@code #offset[2][10][2]}, returns "offset"
         *
         * @return input name
         */
        public String getInput() {
            return input;
        }

        /**
         * The original full input, including arguments e.g. for {@code #offset[2][10][2]}, returns "#offset[2][10][2]"
         *
         * @return original full input
         */
        public String getFull() {
            return full;
        }

        @Override
        public String toString() {
            return input + " | " + and;
        }

    }

}
