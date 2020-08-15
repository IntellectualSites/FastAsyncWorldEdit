package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.noise.SimplexNoiseGenerator;

public class SimplexPatternParser extends NoisePatternParser {
    private static final String SIMPLEX_NAME = "simplex";

    public SimplexPatternParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_NAME, SimplexNoiseGenerator::new);
    }
}
