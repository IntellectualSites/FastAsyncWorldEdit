package com.fastasyncworldedit.core.object.pattern.parser;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.object.pattern.BufferedPattern;
import com.sk89q.worldedit.WorldEdit;
import com.fastasyncworldedit.core.object.parser.RichParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class BufferedPatternParser extends RichParser<Pattern> {

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public BufferedPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#buffer");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index) {
        if (index == 0) {
            return this.worldEdit.getPatternFactory().getSuggestions(argumentInput).stream();
        }
        return Stream.empty();
    }

    @Override
    protected Pattern parseFromInput(@NotNull String[] arguments, ParserContext context) throws InputParseException {
        if (arguments.length != 1) {
            throw new InputParseException(Caption.of("fawe.error.command.syntax",
                    TextComponent.of(getPrefix() + "[pattern] (e.g. " + getPrefix() + "[stone,dirt])")));
        }
        Pattern inner = this.worldEdit.getPatternFactory().parseFromInput(arguments[0], context);
        return new BufferedPattern(context.requireActor(), inner);
    }
}
