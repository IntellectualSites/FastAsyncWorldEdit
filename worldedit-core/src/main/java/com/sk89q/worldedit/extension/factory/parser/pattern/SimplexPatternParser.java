package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.boydti.fawe.object.random.SimplexRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class SimplexPatternParser extends RichParser<Pattern> {
    private static final String SIMPLEX_PREFIX = "#simplex";

    public SimplexPatternParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_PREFIX);
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0) {
            if (argumentInput.isEmpty()) {
                return Stream.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
            }
            // if already a valid number, suggest more digits
            if (isDouble(argumentInput)) {
                Stream<String> numbers = Stream.of("", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
                if (argumentInput.indexOf('.') == -1) {
                    numbers = Stream.concat(numbers, Stream.of("."));
                }
                return numbers.map(s -> argumentInput + s);
            }
            // no valid input anymore
            return Stream.empty();
        }
        if (index == 1) {
            return worldEdit.getPatternFactory().getSuggestions(argumentInput).stream();
        }
        return Stream.empty();
    }

    @Override
    protected Pattern parseFromInput(@NotNull String[] arguments, ParserContext context) {
        if (arguments.length != 2) {
            throw new InputParseException("Simplex requires a scale and a pattern, e.g. #simplex[5][dirt,stone]");
        }
        double scale = Double.parseDouble(arguments[0]);
        scale = 1d / Math.max(1, scale);
        Pattern inner = worldEdit.getPatternFactory().parseFromInput(arguments[1], context);
        if (inner instanceof RandomPattern) {
            return new RandomPattern(new SimplexRandom(scale), (RandomPattern) inner);
        } else if (inner instanceof BlockStateHolder) {
            return inner; // single blocks won't have any impact on how simplex behaves
        } else {
            throw new InputParseException("Pattern " + inner.getClass().getSimpleName() + " cannot be used with #simplex");
        }
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
