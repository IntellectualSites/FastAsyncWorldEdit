package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.SaturatePattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class SaturatePatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public SaturatePatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#saturate");
    }

    @Override
    public Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index > 3) {
            return Stream.empty();
        }
        return SuggestionHelper.suggestPositiveIntegers(argumentInput);
    }

    @Override
    public Pattern parseFromInput(@Nonnull String[] input, ParserContext context) throws InputParseException {
        if (input.length != 4) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[r][g][b][a] (e.g. " + getPrefix() + "[156][100][0][120])")
            ));
        }
        return new SaturatePattern(context.requireExtent(), context.requireSession(),
                Integer.parseInt(input[0]),
                Integer.parseInt(input[1]),
                Integer.parseInt(input[2]),
                Integer.parseInt(input[3])
        );
    }

}
