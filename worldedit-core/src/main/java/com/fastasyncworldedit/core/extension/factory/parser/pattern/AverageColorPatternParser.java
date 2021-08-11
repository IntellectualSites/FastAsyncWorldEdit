package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.AverageColorPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class AverageColorPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public AverageColorPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#averagecolor", "#averagecolour");
    }

    @Override
    public Stream<String> getSuggestions(String argumentInput, int index) {
        if (index != 0) {
            return Stream.empty();
        }
        return SuggestionHelper.suggestPositiveIntegers(argumentInput);
    }

    @Override
    public Pattern parseFromInput(@Nonnull String[] input, ParserContext context) throws InputParseException {
        if (input.length != 1) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[color] (e.g. " + getPrefix() + "[156])")
            ));
        }
        return new AverageColorPattern(context.requireExtent(), context.requireSession(), Integer.parseInt(input[0]));
    }

}
