package com.fastasyncworldedit.core.extension.factory.parser.mask;

import com.fastasyncworldedit.core.extension.factory.parser.common.HotbarParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.Mask;

public class HotbarMaskParser extends HotbarParser<Mask> {

    public HotbarMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Mask parseFromSimpleInput(String input, ParserContext context) {
        return new BlockTypeMask(context.getExtent(), getBlockTypes(context));
    }

}
