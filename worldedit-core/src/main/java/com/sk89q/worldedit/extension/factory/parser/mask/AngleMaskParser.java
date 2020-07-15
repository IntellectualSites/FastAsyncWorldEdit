package com.sk89q.worldedit.extension.factory.parser.mask;

import com.boydti.fawe.object.mask.AngleMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class AngleMaskParser extends RichParser<Mask> {

    public AngleMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "/");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0 || index == 1) {
            return suggestPositiveDoubles(argumentInput).flatMap(s -> Stream.of(s, s + "d"));
        }
        return null;
    }

    @Override
    protected Mask parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 2) {
            return null;
        }
        String minArg = arguments[0];
        String maxArg = arguments[1];
        boolean degree = minArg.endsWith("d");
        if (degree ^ maxArg.endsWith("d")) {
            throw new InputParseException("Cannot combine degree with block-step");
        }
        double min;
        double max;
        if (degree) {
            double minDeg = Double.parseDouble(minArg.substring(0, minArg.length() - 1));
            double maxDeg = Double.parseDouble(maxArg.substring(0, maxArg.length() - 1));
            min = (Math.tan(minDeg * (Math.PI / 180)));
            max = (Math.tan(maxDeg * (Math.PI / 180)));
        } else {
            min = Double.parseDouble(minArg);
            max = Double.parseDouble(maxArg);
        }

        return new AngleMask(context.getExtent(), min, max, false, 1);
    }
}
