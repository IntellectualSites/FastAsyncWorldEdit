package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.noise.RidgedMultiFractalNoise;

public class RidgedMultiFractalPatternParser extends NoisePatternParser {

    private static final String RIDGED_MULTI_FRACTAL_NAME = "rmf";

    /**
     * Create a new ridged multi fractal noise parser.
     *
     * @param worldEdit the worldedit instance.
     */
    public RidgedMultiFractalPatternParser(WorldEdit worldEdit) {
        super(worldEdit, RIDGED_MULTI_FRACTAL_NAME, RidgedMultiFractalNoise::new);
    }
}
