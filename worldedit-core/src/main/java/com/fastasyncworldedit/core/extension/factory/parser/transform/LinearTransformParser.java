package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.LinearTransform;
import com.fastasyncworldedit.core.extent.transform.RandomTransform;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class LinearTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public LinearTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#linear", "#l");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index == 0) {
            return worldEdit.getTransformFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 1) {
            return null;
        }
        ResettableExtent inner = worldEdit.getTransformFactory().parseFromInput(arguments[0], context);
        if (inner instanceof RandomTransform) {
            return new LinearTransform(((RandomTransform) inner).getExtents().toArray(new ResettableExtent[0]));
        }
        return inner; // TODO what about non-random transforms?
    }

}
