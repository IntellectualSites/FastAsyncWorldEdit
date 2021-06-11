package com.fastasyncworldedit.regions.general;

public interface RegionFilter {

    boolean containsRegion(int mcaX, int mcaZ);

    boolean containsChunk(int chunkX, int chunkZ);
}
