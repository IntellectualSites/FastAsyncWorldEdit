package com.fastasyncworldedit.core.regions.filter;

public interface RegionFilter {

    boolean containsRegion(int mcaX, int mcaZ);

    boolean containsChunk(int chunkX, int chunkZ);

}
