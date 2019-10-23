package com.boydti.fawe.command;

import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.InputParser;

import java.util.*;

public abstract class FaweParser<T> extends InputParser<T> {

    protected FaweParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    public PlatformCommandManager getPlatform() {
        return PlatformCommandManager.getInstance();
    }

    public T parse(String input, Actor actor) {
        return getPlatform().parse("pattern " + input, actor);
    }

    public T catchSuggestion(String currentInput, String nextInput, ParserContext context) throws InputParseException {
        try {
            return parseFromInput(nextInput, context);
        } catch (SuggestInputParseException e) {
            e.prepend(currentInput.substring(0, currentInput.length() - nextInput.length()));
            throw e;
        }
    }

    protected static class ParseEntry {
        public boolean and;
        public String input;
        public String full;

        public ParseEntry(String full, String input, boolean type) {
            this.full = full;
            this.input = input;
            this.and = type;
        }

        @Override
        public String toString() {
            return input + " | " + and;
        }
    }

    public static List<Map.Entry<ParseEntry, List<String>>> parse(String toParse) throws InputParseException {
        List<Map.Entry<ParseEntry, List<String>>> keys = new ArrayList<>();
        List<String> inputs = new ArrayList<>();
        List<Boolean> and = new ArrayList<>();
        int last = 0;
        outer:
        for (int i = 0; i < toParse.length(); i++) {
            char c = toParse.charAt(i);
            switch (c) {
                case ',':
                case '&':
                    String result = toParse.substring(last, i);
                    if (!result.isEmpty()) {
                        inputs.add(result);
                        and.add(c == '&');
                    } else {
                        throw new InputParseException("Invalid dangling character " + c);
                    }
                    last = i + 1;
                    continue outer;
                default:
                    if (c == '[' && StringMan.getMatchingBracket(c) != c) {
                        int next = StringMan.findMatchingBracket(toParse, i);
                        if (next != -1) {
                            i = next;
                        } else {
                            toParse += "]";
                            i = toParse.length();
                        }
                        continue outer;
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
                if (startPos == -1) break;
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
}
