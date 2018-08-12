package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;

public abstract class GenBase {

    private final int checkAreaSize;
    private final PseudoRandom random;
    private final long seed;
    private final long worldSeed1, worldSeed2;
    private MutableBlockVector2D mutable = new MutableBlockVector2D();

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

    public void generate(Vector2D chunkPos, Extent chunk) throws WorldEditException {
        int i = this.checkAreaSize;
        int chunkX = chunkPos.getBlockX();
        int chunkZ = chunkPos.getBlockZ();

        for (int x = chunkX - i; x <= chunkX + i; x++) {
            mutable.mutX(x);
            for (int z = chunkZ - i; z <= chunkZ + i; z++) {
                mutable.mutZ(z);
                this.random.setSeed(worldSeed1 * x ^ worldSeed2 * z ^ seed);
                generateChunk(mutable, chunkPos, chunk);
            }
        }
    }

    public abstract void generateChunk(Vector2D adjacentChunk, Vector2D originChunk, Extent chunk) throws WorldEditException;
}
