package com.sk89q.worldedit.extension.factory.parser.mask;

import com.fastasyncworldedit.core.object.mask.RadiusMask;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.factory.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class RadiusMaskParser extends RichParser<Mask> {

    public RadiusMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "{");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0 || index == 1) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length < 2) {
            return null;
        }
        int min = Integer.parseInt(arguments[0]);
        int max = Integer.parseInt(arguments[1]);
        return new RadiusMask(min, max);
    }
}
