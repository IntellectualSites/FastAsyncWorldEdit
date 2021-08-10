package com.fastasyncworldedit.core.extension.factory.parser.transform;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.extent.transform.ScaleTransform;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ScaleTransformParser extends RichParser<ResettableExtent> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public ScaleTransformParser(WorldEdit worldEdit) {
        super(worldEdit, "#scale");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index < 3) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        }
        return Stream.empty();
    }

    @Override
    protected ResettableExtent parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        double xScale;
        double yScale;
        double zScale;
        if (arguments.length == 1) {
            xScale = yScale = zScale = Double.parseDouble(arguments[0]);
        } else if (arguments.length == 3) {
            xScale = Double.parseDouble(arguments[0]);
            yScale = Double.parseDouble(arguments[1]);
            zScale = Double.parseDouble(arguments[2]);
        } else {
            return null;
        }
        return new ScaleTransform(context.requireExtent(), xScale, yScale, zScale);
    }

}
