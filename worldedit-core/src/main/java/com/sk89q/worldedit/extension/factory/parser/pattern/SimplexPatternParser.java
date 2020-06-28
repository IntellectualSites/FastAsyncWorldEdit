package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.boydti.fawe.object.random.SimplexRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;

import java.util.stream.Stream;

public class SimplexPatternParser extends RichParser<Pattern> {
    private static final String SIMPLEX_PREFIX = "#simplex";

    public SimplexPatternParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_PREFIX);
    }

    @Override
    protected Stream<String> getSuggestions(String argument, int index) {
        if (index == 0) {
            if (argument.isEmpty()) {
                return Stream.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
            }
            // if already a valid number, suggest more digits
            if (isDouble(argument)) {
                Stream<String> numbers = Stream.of("", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
                if (argument.indexOf('.') == -1) {
                    numbers = Stream.concat(numbers, Stream.of("."));
                }
                return numbers.map(s -> argument + s);
            }
            // no valid input anymore
            return Stream.empty();
        }
        if (index == 1) {
            return worldEdit.getPatternFactory().getSuggestions(argument).stream();
        }
        return Stream.empty();
    }

    @Override
    protected Pattern parseFromInput(String[] arguments, ParserContext context) {
        if (arguments.length != 2) {
            // TODO
            throw new InputParseException("Invalid amount of arguments. Syntax: #simplex[scale][pattern]");
        }
        double scale = Double.parseDouble(arguments[0]);
        scale = 1d / Math.max(1, scale);
        Pattern inner = worldEdit.getPatternFactory().parseFromInput(arguments[1], context);
        if (inner instanceof RandomPattern) {
            return new RandomPattern(new SimplexRandom(scale), (RandomPattern) inner);
        }
        // TODO handle RandomStatePattern?
        // TODO for other patterns, either warn actor (inner pattern will be returned) or throw exception
        return inner;
    }

    private static boolean isDouble(String input) {
        boolean point = false;
        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c)) {
                if (c == '.' && !point) {
                    point = true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
