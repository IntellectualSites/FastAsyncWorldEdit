package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IGetBlocks;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.blocks.CharSetBlocks;
import com.boydti.fawe.beta.implementation.holder.ChunkHolder;

public class BukkitChunkHolder extends ChunkHolder<Boolean> {
    @Override
    public void init(final IQueueExtent extent, final int X, final int Z) {
        super.init(extent, X, Z);
    }

    @Override
    public IGetBlocks get() {
        BukkitQueue extent = (BukkitQueue) getExtent();
        return new BukkitGetBlocks(extent.getNmsWorld(), getX(), getZ());
    }

    @Override
    public boolean applyAsync() {
        BukkitGetBlocks get = (BukkitGetBlocks) cachedGet();
        CharSetBlocks set = (CharSetBlocks) cachedSet();
//        - getBlocks
//            - set lock
//            - synchronize on chunk object
//            - verify section is same object as chunk's section
//            - merge with setblocks
//        - set section
//            - verify chunk is same
//            - verify section is same
//            - Otherwise repeat steps on main thread
//            - set status to needs relighting
//        - mark as dirty
//        - skip verification if main thread
        throw new UnsupportedOperationException("Not implemented");
//        return true;
    }

    @Override
    public boolean applySync() {
        return true;
    }

    @Override
    public void set(final Filter filter) {
        // for each block
        // filter.applyBlock(block)
        throw new UnsupportedOperationException("Not implemented");
    }
}