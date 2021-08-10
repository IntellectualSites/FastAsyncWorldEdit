package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.noise.VoronoiNoise;

public class VoronoiPatternParser extends NoisePatternParser {

    private static final String VORONOI_NAME = "voronoi";

    /**
     * Create a new voronoi noise parser.
     *
     * @param worldEdit the worldedit instance.
     */
    public VoronoiPatternParser(WorldEdit worldEdit) {
        super(worldEdit, VORONOI_NAME, VoronoiNoise::new);
    }

}
