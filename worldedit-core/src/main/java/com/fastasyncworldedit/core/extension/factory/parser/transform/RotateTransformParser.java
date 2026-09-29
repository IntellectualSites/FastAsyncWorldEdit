package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.transform.AffineTransform;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class RotateTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public RotateTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#rotate");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        if (index == 3) {
            return worldEdit.getTransformFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        AffineTransform transform = new AffineTransform();
        Extent extent;
        if (arguments.length == 1) {
            transform = transform.rotateY(Double.parseDouble(arguments[0]));
            extent = context.requireExtent();
        } else if (arguments.length == 3 || arguments.length == 4) {
            transform = transform.rotateX(Double.parseDouble(arguments[0]));
            transform = transform.rotateY(Double.parseDouble(arguments[1]));
            transform = transform.rotateZ(Double.parseDouble(arguments[2]));
            extent = arguments.length == 4 ? worldEdit.getTransformFactory().parseFromInput(arguments[3], context) :
                    context.requireExtent();
        } else {
            return null;
        }
        return new BlockTransformExtent(extent, transform);
    }

}
