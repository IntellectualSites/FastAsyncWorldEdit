package com.boydti.fawe.jnbt.anvil;

public class ChunkSimplifier {
    private final HeightMapMCAGenerator gen;

    public ChunkSimplifier(HeightMapMCAGenerator gen) {
        this.gen = gen;
    }

    public void simplify(MCAChunk chunk) {
        // Copy biome
        // Calculate water level
        // Determine floor block
        // Determine overlay block
        // determine main block
        // Copy bedrock
        // copy anomalies to blocks
    }
}
