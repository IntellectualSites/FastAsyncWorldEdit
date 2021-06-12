package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.object.pattern.ExistingPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import java.util.Collections;
import java.util.List;

public class ExistingPatternParser extends SimpleInputParser<Pattern> {
    private final List<String> aliases = Collections.singletonList("#existing");

    public ExistingPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return this.aliases;
    }

    @Override
    public Pattern parseFromSimpleInput(String input, ParserContext context) throws InputParseException {
        return new ExistingPattern(context.requireExtent());
    }
}
