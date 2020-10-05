package com.sk89q.worldedit.extension.factory.parser.mask;

import com.boydti.fawe.object.mask.WallMask;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.List;

public class WallMaskParser extends SimpleInputParser<Mask> {

    private final List<String> aliases = ImmutableList.of("#wall");

    public WallMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public Mask parseFromSimpleInput(String input, ParserContext context) throws InputParseException {
        return new MaskIntersection(new ExistingBlockMask(context.getExtent()), new WallMask(new BlockMask(context.getExtent(), BlockTypes.AIR.getDefaultState().toBaseBlock()), 1, 8));
    }
}
