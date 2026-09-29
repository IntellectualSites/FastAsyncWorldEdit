package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.mask.AdjacentAnyMask;
import com.fastasyncworldedit.core.function.mask.AdjacentMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class AdjacentMaskParser extends RichParser<Mask> {

    public AdjacentMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "~");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0) {
            return worldEdit.getMaskFactory().getSuggestions(argumentInput, context).stream();
        } else if (index == 1 || index == 2) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length == 0) {
            return null;
        }
        Mask subMask = worldEdit.getMaskFactory().parseFromInput(arguments[0], context);
        int min = arguments.length > 1 ? Integer.parseInt(arguments[1]) : -1;
        int max = arguments.length > 2 ? Integer.parseInt(arguments[2]) : -1;
        if (min == -1 && max == -1) {
            min = 1;
            max = 8;
        } else if (max == -1) {
            max = min;
        }
        if (max >= 8 && min == 1) {
            return new AdjacentAnyMask(subMask, context.getMinY(), context.getMaxY());
        }
        return new AdjacentMask(subMask, min, max);
    }

}
