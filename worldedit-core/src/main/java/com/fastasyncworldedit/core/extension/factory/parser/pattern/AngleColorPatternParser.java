package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.function.pattern.AngleColorPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class AngleColorPatternParser extends SimpleInputParser<Pattern> {

    private static final List<String> aliases = Arrays.asList("#anglecolor", "#anglecolour");

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public AngleColorPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public Stream<String> getSuggestions(String argumentInput) {
        return SuggestionHelper.suggestPositiveIntegers(argumentInput);
    }

    @Override
    public Pattern parseFromSimpleInput(@Nonnull String input, ParserContext context) throws InputParseException {
        int distance = Integer.parseInt(input);
        return new AngleColorPattern(context.requireExtent(), context.requireSession(), distance);
    }

}
