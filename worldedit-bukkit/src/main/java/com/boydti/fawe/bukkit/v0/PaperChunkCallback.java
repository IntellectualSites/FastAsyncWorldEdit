package com.boydti.fawe.bukkit.v0;

import org.bukkit.Chunk;
import org.bukkit.World;

public abstract class PaperChunkCallback {
    public PaperChunkCallback(World world, int x, int z) {
//        world.getChunkAtAsync(x, z, PaperChunkCallback.this::onLoad);
        throw new UnsupportedOperationException("Not implemented");
    }

    public abstract void onLoad(Chunk chunk);
}
