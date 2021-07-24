package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.math.random.SimplexNoiseGenerator;
import com.sk89q.worldedit.WorldEdit;

public class SimplexPatternParser extends NoisePatternParser {

    private static final String SIMPLEX_NAME = "simplex";

    public SimplexPatternParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_NAME, SimplexNoiseGenerator::new);
    }

}
