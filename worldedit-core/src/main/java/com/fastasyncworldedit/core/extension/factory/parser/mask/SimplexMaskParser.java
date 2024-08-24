package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.mask.SimplexMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class SimplexMaskParser extends RichParser<Mask> {

    private static final String SIMPLEX_PREFIX = "#simplex";

    public SimplexMaskParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_PREFIX);
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
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
