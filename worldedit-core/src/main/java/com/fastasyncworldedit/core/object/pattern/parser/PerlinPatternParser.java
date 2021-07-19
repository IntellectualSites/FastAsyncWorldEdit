package com.fastasyncworldedit.core.object.pattern.parser;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.noise.PerlinNoise;

public class PerlinPatternParser extends NoisePatternParser {
    private static final String PERLIN_NAME = "perlin";

    /**
     * Create a new perlin noise parser.
     *
     * @param worldEdit the worldedit instance.
     */
    public PerlinPatternParser(WorldEdit worldEdit) {
        super(worldEdit, PERLIN_NAME, PerlinNoise::new);
    }
}
