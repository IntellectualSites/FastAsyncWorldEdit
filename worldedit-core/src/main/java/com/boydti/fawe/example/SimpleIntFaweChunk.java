package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;

public class SimpleIntFaweChunk extends IntFaweChunk {

    public SimpleIntFaweChunk(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public SimpleIntFaweChunk(FaweQueue parent, int x, int z, int[][] ids, short[] count, short[] air) {
        super(parent, x, z, ids, count, air);
    }

    @Override
    public Object getNewChunk() {
        return this;
    }

    @Override
    public IntFaweChunk copy(boolean shallow) {
        SimpleIntFaweChunk copy;
        if (shallow) {
            copy = new SimpleIntFaweChunk(getParent(), getX(), getZ(), setBlocks, count, air);
            copy.biomes = biomes;
        } else {
            copy = new SimpleIntFaweChunk(getParent(), getX(), getZ(), (int[][]) MainUtil.copyNd(setBlocks), count.clone(), air.clone());
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