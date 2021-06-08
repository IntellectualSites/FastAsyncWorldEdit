package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.object.pattern.Linear2DBlockPattern;
import com.google.common.base.Preconditions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.factory.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Stream;

public class Linear2DPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public Linear2DPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#linear2d", "#l2d");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        switch (index) {
            case 0:
                return this.worldEdit.getPatternFactory().getSuggestions(argumentInput).stream();
            case 1:
            case 2:
                return SuggestionHelper.suggestPositiveIntegers(argumentInput);
            default:
                return Stream.empty();
        }
    }

    @Override
    protected Pattern parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length == 0 || arguments.length > 3) {
            throw new InputParseException(Caption.of("fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[pattern] (e.g. " + getPrefix() + "[stone,dirt])")));
        }
        Pattern inner = this.worldEdit.getPatternFactory().parseFromInput(arguments[0], context);
        if (inner instanceof BlockStateHolder) {
            return inner;
        }
        int xScale = 1;
        int zScale = 1;
        if (arguments.length > 1) {
            xScale = Integer.parseInt(arguments[1]);
            Preconditions.checkArgument(xScale != 0);
        }
        if (arguments.length > 2) {
            zScale = Integer.parseInt(arguments[2]);
            Preconditions.checkArgument(zScale != 0);
        }
        if (inner instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) inner).getPatterns();
            return new Linear2DBlockPattern(patterns.toArray(new Pattern[0]), xScale, zScale);
        }
        throw new InputParseException(TextComponent.of("Pattern " + inner.getClass().getSimpleName()
                + " cannot be used with " + getPrefix()));
    }
}
