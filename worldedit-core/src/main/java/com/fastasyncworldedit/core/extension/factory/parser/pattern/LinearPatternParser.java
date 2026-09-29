package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.LinearBlockPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Stream;

public class LinearPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public LinearPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#linear", "#l");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        return switch (index) {
            case 0 -> this.worldEdit.getPatternFactory().getSuggestions(argumentInput, context).stream();
            case 1, 2, 3 -> SuggestionHelper.suggestPositiveIntegers(argumentInput);
            default -> Stream.empty();
        };
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 1) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[pattern] (e.g. " + getPrefix() + "[stone,dirt])")
            ));
        }
        Pattern inner = this.worldEdit.getPatternFactory().parseFromInput(arguments[0], context);
        if (inner instanceof BlockStateHolder) {
            return inner;
        }
        if (inner instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) inner).getPatterns();
            return new LinearBlockPattern(patterns.toArray(new Pattern[0]));
        }
        throw new InputParseException(TextComponent.of("Pattern " + inner.getClass().getSimpleName()
                + " cannot be used with " + getPrefix()));
    }

}
