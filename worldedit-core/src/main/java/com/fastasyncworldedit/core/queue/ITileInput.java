package com.fastasyncworldedit.core.queue;

import com.sk89q.jnbt.CompoundTag;

public interface ITileInput {
    CompoundTag getTile(int x, int y, int z);
}
