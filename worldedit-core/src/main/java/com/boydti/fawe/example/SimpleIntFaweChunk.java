package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;

public class SimpleIntFaweChunk extends IntFaweChunk {

    public SimpleIntFaweChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public SimpleIntFaweChunk(FaweQueue parent, int x, int z, int[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public Object getNewChunk() {
        return this;
    }

    @Override
    public IntFaweChunk copy(boolean shallow) {
        SimpleIntFaweChunk copy;
        if (shallow) {
            copy = new SimpleIntFaweChunk(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
        } else {
            copy = new SimpleIntFaweChunk(getParent(), getX(), getZ(), (int[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
        }
        return copy;
    }

    @Override
    public FaweChunk call() {
        getParent().setChunk(this);
        return this;
    }
}
