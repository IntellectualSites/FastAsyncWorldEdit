package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.Linear3DTransform;
import com.fastasyncworldedit.core.extent.transform.RandomTransform;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class Linear3DTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public Linear3DTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#linear3d", "#l3d");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0) {
            return worldEdit.getTransformFactory().getSuggestions(argumentInput).stream();
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 1) {
            return null;
        }
        ResettableExtent inner = worldEdit.getTransformFactory().parseFromInput(arguments[0], context);
        if (inner instanceof RandomTransform) {
            return new Linear3DTransform(((RandomTransform) inner).getExtents().toArray(new ResettableExtent[0]));
        }
        return inner; // TODO what about non-random transforms?
    }

}
