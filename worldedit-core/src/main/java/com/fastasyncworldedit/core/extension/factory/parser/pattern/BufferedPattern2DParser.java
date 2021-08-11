package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.function.pattern.BufferedPattern2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class BufferedPattern2DParser extends SimpleInputParser<Pattern> {

    private static final List<String> aliases = Collections.singletonList("#buffer2d");

    /**
     * Create a new rich parser with a defined prefix for the result, e.g. {@code #simplex}.
     *
     * @param worldEdit the worldedit instance.
     */
    public BufferedPattern2DParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public Stream<String> getSuggestions(String argumentInput) {
        return this.worldEdit.getPatternFactory().getSuggestions(argumentInput).stream();
    }

    @Override
    public Pattern parseFromSimpleInput(@Nonnull String input, ParserContext context) throws InputParseException {
        Pattern inner = this.worldEdit.getPatternFactory().parseFromInput(input, context);
        return new BufferedPattern2D(context.requireActor(), inner);
    }

}
