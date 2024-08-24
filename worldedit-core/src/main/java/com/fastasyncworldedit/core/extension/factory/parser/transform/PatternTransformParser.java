package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.PatternTransform;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class PatternTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public PatternTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#pattern");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0) {
            return worldEdit.getPatternFactory().getSuggestions(argumentInput, context).stream();
        } else if (index == 1) {
            return worldEdit.getTransformFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length > 2) {
            return null;
        }
        Pattern pattern = worldEdit.getPatternFactory().parseFromInput(arguments[0], context);
        Extent extent = arguments.length == 2 ? worldEdit.getTransformFactory().parseFromInput(arguments[1], context) :
                context.requireExtent();
        return new PatternTransform(extent, pattern);
    }

}
