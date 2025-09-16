package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.TypeSwapPattern;
import com.fastasyncworldedit.core.util.Permission;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

public class TypeSwapPatternParser extends RichParser<Pattern> {

    private static final List<String> SUGGESTIONS = List.of("oak", "spruce", "stone", "sandstone");

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public TypeSwapPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#typeswap", "#ts", "#swaptype");
    }

    @Override
    public Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (index > 2) {
            return Stream.empty();
        }
        return SUGGESTIONS.stream();
    }

    @Override
    public Pattern parseFromInput(@Nonnull String[] input, ParserContext context) throws InputParseException {
        if (input.length != 2) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[input][output] (e.g. " + getPrefix() + "[spruce][oak])")
            ));
        }
        return new TypeSwapPattern(
                context.requireExtent(),
                input[0],
                input[1],
                Permission.hasPermission(context.requireActor(), "fawe.pattern.typeswap.regex")
        );
    }

}
