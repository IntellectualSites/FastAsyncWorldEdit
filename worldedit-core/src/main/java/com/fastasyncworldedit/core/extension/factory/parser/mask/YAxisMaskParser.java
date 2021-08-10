package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.function.mask.YAxisMask;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import java.util.List;

public class YAxisMaskParser extends SimpleInputParser<Mask> {

    private final List<String> aliases = ImmutableList.of("#yaxis");

    public YAxisMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public Mask parseFromSimpleInput(String input, ParserContext context) {
        return new YAxisMask(context.getExtent());
    }

}
