package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.ScaleTransform;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class ScaleTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #scale}.
     *
     * @param worldEdit the worldedit instance.
     */
    public ScaleTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#scale");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        } else if (index == 3) {
            return worldEdit.getTransformFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        double xScale;
        double yScale;
        double zScale;
        Extent extent;
        if (arguments.length == 1) {
            xScale = yScale = zScale = Double.parseDouble(arguments[0]);
            extent = context.requireExtent();
        } else if (arguments.length == 3 || arguments.length == 4) {
            xScale = Double.parseDouble(arguments[0]);
            yScale = Double.parseDouble(arguments[1]);
            zScale = Double.parseDouble(arguments[2]);
            extent = arguments.length == 4 ? worldEdit.getTransformFactory().parseFromInput(arguments[3], context) :
                    context.requireExtent();
        } else {
            return null;
        }
        return new ScaleTransform(extent, xScale, yScale, zScale);
    }

}
