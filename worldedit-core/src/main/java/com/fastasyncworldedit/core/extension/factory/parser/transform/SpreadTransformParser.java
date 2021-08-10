package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.transform.RandomOffsetTransform;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class SpreadTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public SpreadTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#spread", "#randomoffset");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveIntegers(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 3) {
            return null;
        }
        int xOffset = Integer.parseInt(arguments[0]);
        int yOffset = Integer.parseInt(arguments[1]);
        int zOffset = Integer.parseInt(arguments[2]);
        return new RandomOffsetTransform(context.requireExtent(), xOffset, yOffset, zOffset);
    }

}
