package com.boydti.fawe.object.collection;

import java.io.Serializable;

public class SparseBlockSet implements Serializable {
    private SparseBitSet[] sets;

    public SparseBlockSet(int depth) {
        sets = new SparseBitSet[depth];
        for (int i = 0; i < sets.length; i++) {
            sets[i] = new SparseBitSet();
        }
    }

    public void setBlock(int index, int id) {
        for (int i = 0; i < sets.length; i++) {
            SparseBitSet set = sets[i];
            if (((id >> i) & 1) == 1) {
                set.set(index);
            } else {
                set.clear(index);
            }
        }
    }

    public int getBlock(int index) {
        int id = 0;
        for (int i = 0; i < sets.length; i++) {
            SparseBitSet set = sets[i];
            if (set.get(index)) {
                id += 1 << i;
            }
        }
        return id;
    }
}