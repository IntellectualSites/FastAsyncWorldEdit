package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.Linear3DBlockPattern;
import com.google.common.base.Preconditions;
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

public class Linear3DPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public Linear3DPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#linear3d", "#l3d");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        return switch (index) {
            case 0 -> this.worldEdit.getPatternFactory().getSuggestions(argumentInput).stream();
            case 1, 2, 3 -> SuggestionHelper.suggestPositiveIntegers(argumentInput);
            default -> Stream.empty();
        };
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length == 0 || arguments.length > 4) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[pattern] (e.g. " + getPrefix() + "[stone,dirt])")
            ));
        }
        Pattern inner = this.worldEdit.getPatternFactory().parseFromInput(arguments[0], context);
        if (inner instanceof BlockStateHolder) {
            return inner;
        }
        int xScale = 1;
        int yScale = 1;
        int zScale = 1;
        if (arguments.length > 1) {
            xScale = Integer.parseInt(arguments[1]);
            Preconditions.checkArgument(xScale != 0);
        }
        if (arguments.length > 2) {
            yScale = Integer.parseInt(arguments[2]);
            Preconditions.checkArgument(yScale != 0);
        }
        if (arguments.length > 3) {
            zScale = Integer.parseInt(arguments[3]);
            Preconditions.checkArgument(zScale != 0);
        }
        if (inner instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) inner).getPatterns();
            return new Linear3DBlockPattern(patterns.toArray(new Pattern[0]), xScale, yScale, zScale);
        }
        throw new InputParseException(TextComponent.of("Pattern " + inner.getClass().getSimpleName()
                + " cannot be used with " + getPrefix()));
    }

}
