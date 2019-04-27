package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.CharGetBlocks;
import com.boydti.fawe.bukkit.v1_13.beta.CharSetBlocks;
import com.boydti.fawe.bukkit.v1_13.beta.IGetBlocks;
import com.boydti.fawe.bukkit.v1_13.beta.ISetBlocks;

public class ChunkHolder extends DelegateChunk<DelegateChunk> {
    public ChunkHolder() {
        super(new InitChunk(null));
        getParent().setParent(this);
    }

    protected final IGetBlocks get() {
        return new CharGetBlocks();
    }

    protected final ISetBlocks set() {
        return new CharSetBlocks();
    }
}
