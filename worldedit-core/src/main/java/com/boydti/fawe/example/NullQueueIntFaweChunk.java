package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.MainUtil;

public class NullQueueIntFaweChunk extends IntFaweChunk {

    public NullQueueIntFaweChunk(int cx, int cz) {
        super(null, cx, cz);
    }

    public NullQueueIntFaweChunk(int x, int z, int[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(null, x, z, ids, count, air, heightMap);
    }

    @Override
    public Object getNewChunk() {
        return null;
    }

    @Override
    public IntFaweChunk copy(boolean shallow) {
        if (shallow) {
            return new NullQueueIntFaweChunk(getX(), getZ(), ids, count, air, heightMap);
        } else {
            return new NullQueueIntFaweChunk(getX(), getZ(), (int[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
        }
    }

    @Override
    public FaweChunk call() {
        return null;
    }
}
