package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.object.number.MutableLong;

public class MCAFilterCounter extends MCAFilter<MutableLong> {
    @Override
    public void finishChunk(MCAChunk chunk, MutableLong cache) {
        cache.add(chunk.getModified());
    }

    @Override
    public MutableLong init() {
        return new MutableLong();
    }

    public long getTotal() {
        long total = 0;
        for (MutableLong value : getAll()) {
            total += value.get();
        }
        return total;
    }
}
