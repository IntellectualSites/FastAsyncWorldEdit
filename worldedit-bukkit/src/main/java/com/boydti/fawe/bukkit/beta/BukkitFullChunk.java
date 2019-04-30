package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.ISetBlocks;
import com.boydti.fawe.beta.implementation.holder.ChunkHolder;

public class BukkitFullChunk extends ChunkHolder {
    public BukkitFullChunk() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void init(IQueueExtent extent, int X, int Z) {

    }

    @Override
    public boolean applyAsync() {
        return false;
    }

    @Override
    public boolean applySync() {
        return false;
    }

    @Override
    public void set(Filter filter) {

    }

    @Override
    public Object get() {
        return null;
    }

    @Override
    public ISetBlocks set() {
        return null;
    }
}
