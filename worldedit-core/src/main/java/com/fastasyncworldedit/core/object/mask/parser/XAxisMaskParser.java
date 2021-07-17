package com.fastasyncworldedit.core.object.mask.parser;

import com.fastasyncworldedit.core.object.mask.XAxisMask;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import java.util.List;

public class XAxisMaskParser extends SimpleInputParser<Mask> {

    private final List<String> aliases = ImmutableList.of("#xaxis");

    public XAxisMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public Mask parseFromSimpleInput(String input, ParserContext context) {
        return new XAxisMask(context.getExtent());
    }
}
