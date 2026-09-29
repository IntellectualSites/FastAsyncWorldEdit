package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.function.pattern.MaskedPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class MaskedPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public MaskedPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#mask");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        return switch (index) {
            case 0 -> this.worldEdit.getMaskFactory().getSuggestions(argumentInput, context).stream();
            case 1, 2 -> this.worldEdit.getPatternFactory().getSuggestions(argumentInput, context).stream();
            default -> Stream.empty();
        };
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 3) {
            throw new InputParseException(Caption.of(
                    "fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[mask][pattern][pattern] (e.g. " + getPrefix() + "[oak_planks][dirt][stone])")
            ));
        }
        Mask mask = this.worldEdit.getMaskFactory().parseFromInput(arguments[0], context);
        Pattern inner1 = this.worldEdit.getPatternFactory().parseFromInput(arguments[1], context);
        Pattern inner2 = this.worldEdit.getPatternFactory().parseFromInput(arguments[2], context);
        return new MaskedPattern(mask, inner1, inner2);
    }

}
