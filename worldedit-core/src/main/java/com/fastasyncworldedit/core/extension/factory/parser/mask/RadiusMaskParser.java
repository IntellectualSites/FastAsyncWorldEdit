package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.mask.RadiusMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class RadiusMaskParser extends RichParser<Mask> {

    public RadiusMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "{");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0 || index == 1) {
            return SuggestionHelper.suggestPositiveIntegers(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 2) {
            return null;
        }
        int min = Integer.parseInt(arguments[0]);
        int max = Integer.parseInt(arguments[1]);
        return new RadiusMask(min, max);
    }

}
