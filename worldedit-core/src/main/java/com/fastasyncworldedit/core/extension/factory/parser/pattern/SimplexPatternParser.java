package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.sk89q.worldedit.WorldEdit;
import com.fastasyncworldedit.core.math.random.SimplexNoiseGenerator;

public class SimplexPatternParser extends NoisePatternParser {
    private static final String SIMPLEX_NAME = "simplex";

    public SimplexPatternParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_NAME, SimplexNoiseGenerator::new);
    }
}
