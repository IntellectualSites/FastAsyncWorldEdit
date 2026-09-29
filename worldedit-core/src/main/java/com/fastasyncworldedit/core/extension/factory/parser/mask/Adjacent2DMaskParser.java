package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.mask.Adjacent2DMask;
import com.fastasyncworldedit.core.function.mask.AdjacentAny2DMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class Adjacent2DMaskParser extends RichParser<Mask> {

    public Adjacent2DMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "~2d", "adjacent2d");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0) {
            return worldEdit.getMaskFactory().getSuggestions(argumentInput).stream();
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
            max = 4;
        } else if (max == -1) {
            max = min;
        }
        if (max >= 4 && min == 1) {
            return new AdjacentAny2DMask(subMask);
        }
        return new Adjacent2DMask(subMask, min, max);
    }

}
