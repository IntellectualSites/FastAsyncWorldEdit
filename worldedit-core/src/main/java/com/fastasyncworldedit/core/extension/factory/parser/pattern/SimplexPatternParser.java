package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.math.random.SimplexNoiseGenerator;
import com.sk89q.worldedit.WorldEdit;

public class SimplexPatternParser extends NoisePatternParser {

    private static final String SIMPLEX_NAME = "simplex";

    /**
     * Create a new rich pattern parser with a defined prefix for the result.
     *
     * @param worldEdit the worldedit instance.
     */
    public SimplexPatternParser(WorldEdit worldEdit) {
        super(worldEdit, SIMPLEX_NAME, SimplexNoiseGenerator::new);
    }

}
