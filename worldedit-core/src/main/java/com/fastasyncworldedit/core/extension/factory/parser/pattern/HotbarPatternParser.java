package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.extension.factory.parser.common.HotbarParser;
import com.fastasyncworldedit.core.math.random.TrueRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.world.block.BlockType;

public class HotbarPatternParser extends HotbarParser<Pattern> {

    public HotbarPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Pattern parseFromSimpleInput(String input, ParserContext context) {
        RandomPattern random = new RandomPattern(new TrueRandom());
        for (BlockType type : getBlockTypes(context)) {
            random.add(type, 1);
        }
        return random;
    }

}
