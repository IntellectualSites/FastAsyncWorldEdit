package com.fastasyncworldedit.core.queue;

import com.sk89q.jnbt.CompoundTag;

@Deprecated(forRemoval = true, since = "2.11.2")
public interface ITileInput {

    CompoundTag getTile(int x, int y, int z);

}
