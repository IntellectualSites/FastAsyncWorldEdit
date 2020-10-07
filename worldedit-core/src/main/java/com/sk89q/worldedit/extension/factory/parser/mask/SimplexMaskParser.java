package com.sk89q.worldedit.extension.factory.parser.mask;

import com.boydti.fawe.object.mask.SimplexMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class SimplexMaskParser extends RichParser<Mask> {
    private static final String SIMPLEX_PREFIX = "#simplex";

    public SimplexMaskParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_PREFIX);
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index < 3) {
            suggestPositiveDoubles(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 3) {
            return null;
        }
        double scale = Double.parseDouble(arguments[0]);
        double min = Double.parseDouble(arguments[1]);
        double max = Double.parseDouble(arguments[2]);
        scale = 1d / Math.max(1d, scale);
        min = (min - 50d) / 50d;
        max = (max - 50d) / 50d;
        return new SimplexMask(scale, min, max);
    }
}
