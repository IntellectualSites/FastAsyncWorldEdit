package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;

public abstract class GenBase {

    private final int checkAreaSize;
    private final PseudoRandom random;
    private final long seed;
    private final long worldSeed1, worldSeed2;

    public GenBase(int area) {
        this.random = new PseudoRandom();
        this.checkAreaSize = area;
        this.seed = PseudoRandom.random.nextLong();
        this.worldSeed1 = PseudoRandom.random.nextLong();
        this.worldSeed2 = PseudoRandom.random.nextLong();
    }

    public int getCheckAreaSize() {
        return checkAreaSize;
    }

    public PseudoRandom getRandom() {
        return random;
    }

    public void generate(BlockVector2 chunkPos, Extent chunk) throws WorldEditException {
        int i = this.checkAreaSize;
        int chunkX = chunkPos.getBlockX();
        int chunkZ = chunkPos.getBlockZ();
        for (int x = chunkX - i; x <= chunkX + i; x++) {
            for (int z = chunkZ - i; z <= chunkZ + i; z++) {
                this.random.setSeed(worldSeed1 * x ^ worldSeed2 * z ^ seed);
                generateChunk(x, z, chunkPos, chunk);
            }
        }
    }

    public abstract void generateChunk(int x, int z, BlockVector2 originChunk, Extent chunk) throws WorldEditException;
}
