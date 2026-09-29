package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.SurfaceRandomOffsetPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class SurfaceRandomOffsetPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public SurfaceRandomOffsetPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#surfacespread");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        return switch (index) {
            case 0 -> this.worldEdit.getPatternFactory().getSuggestions(argumentInput, context).stream();
            case 1 -> SuggestionHelper.suggestPositiveIntegers(argumentInput);
            default -> Stream.empty();
        };
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 2) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[pattern] (e.g. " + getPrefix() + "[stone][2])")
            ));
        }
        Pattern inner = this.worldEdit.getPatternFactory().parseFromInput(arguments[0], context);
        int distance = Integer.parseInt(arguments[1]);
        return new SurfaceRandomOffsetPattern(inner, distance, context.getMinY(), context.getMaxY());
    }

}
