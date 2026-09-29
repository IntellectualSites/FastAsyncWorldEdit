package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.OffsetTransform;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class OffsetTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public OffsetTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#offset");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveIntegers(argumentInput);
        } else if (index == 3) {
            return worldEdit.getTransformFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 3 && arguments.length != 4) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of("#offset[x][y][z]")
            ));
        }
        int xOffset = Integer.parseInt(arguments[0]);
        int yOffset = Integer.parseInt(arguments[1]);
        int zOffset = Integer.parseInt(arguments[2]);
        Extent extent;
        extent = arguments.length == 4 ? worldEdit.getTransformFactory().parseFromInput(arguments[3], context) :
                context.requireExtent();
        return new OffsetTransform(extent, xOffset, yOffset, zOffset);
    }

}
