package com.boydti.fawe.regions.general;

public interface RegionFilter {
    public boolean containsRegion(int mcaX, int mcaZ);

    public boolean containsChunk(int chunkX, int chunkZ);
}