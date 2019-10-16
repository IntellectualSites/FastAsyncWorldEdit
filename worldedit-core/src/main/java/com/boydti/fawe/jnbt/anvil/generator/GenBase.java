package com.boydti.fawe.jnbt.anvil.generator;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;

import java.util.concurrent.ThreadLocalRandom;

public abstract class GenBase {

    private final int checkAreaSize;
    private final long seed;
    private final long worldSeed1, worldSeed2;

    public GenBase(int area) {
        this.checkAreaSize = area;
        this.seed = ThreadLocalRandom.current().nextLong();
        this.worldSeed1 = ThreadLocalRandom.current().nextLong();
        this.worldSeed2 = ThreadLocalRandom.current().nextLong();
    }

    public int getCheckAreaSize() {
        return checkAreaSize;
    }

    public void generate(BlockVector2 chunkPos, Extent chunk) throws WorldEditException {
        int i = this.checkAreaSize;
        int chunkX = chunkPos.getBlockX();
        int chunkZ = chunkPos.getBlockZ();
        for (int x = chunkX - i; x <= chunkX + i; x++) {
            for (int z = chunkZ - i; z <= chunkZ + i; z++) {
                generateChunk(x, z, chunkPos, chunk);
            }
        }
    }

    public abstract void generateChunk(int x, int z, BlockVector2 originChunk, Extent chunk) throws WorldEditException;
}
